# Sprint — Implementation Plan
### A context-aware, AI-augmented time & task tracker (Android-first, expanding to Windows via KMP)

---

## 0. Project Summary

**What it is:** A local-first productivity tracker that automatically classifies your time across contexts (Internship, Coursework, Side Projects, Life) using foreground-app tracking, presents it as a personal Kanban board mirroring real Scrum sprints, and generates a weekly AI "retro" using an actor-critic agent pair.

**Why it's worth building:** Fills real gaps in your portfolio — Kotlin Multiplatform, systems-level tracking (UsageStatsManager, cross-platform window daemons), sync architecture, and a genuine testing/CI story. Reuses your existing actor-critic LLM pattern from Corvus/Falco applied to a new domain.

**Build order:** Android-only MVP first (single device, no sync) → then multi-device sync → then Windows desktop via KMP.

---

## 0.1 User Journey Overview

On a normal day, you interact deliberately with the app for **well under a minute total** — everything else is passive.

| Moment | You do | You see | Behind the scenes |
|---|---|---|---|
| Morning | Nothing | Widget shows yesterday's breakdown | Passive — no action triggered |
| Workday | Nothing (usually) | App switches invisible | `UsageStatsManager`/window daemon logs raw sessions; rule-filter + actor-critic classify most automatically |
| Occasional (1–3x/day) | Swipe a review card | *"32 min Chrome — which context?"* | Session was mid-confidence; your answer also updates the rule table for next time |
| Physical activity (Phase 12) | Swipe a review card | *"45 min Gym, running — log as Workout?"* | Geofence + Activity Recognition combined, always confirmed manually (never auto-committed) |
| Evening | Move a Kanban card (~30s) | Task board updates | Local write, stamped for later sync conflict resolution |
| Sunday | Open one notification | One headline insight + bar chart | Retro Actor drafts, Retro Critic fact-checks before it's ever shown to you |

**Design constraint to hold onto throughout the build:** if any phase starts requiring more taps than this table implies, that's a signal it drifted from the goal — the app is meant to be glanced at and occasionally confirmed, not opened and operated.

---

## 1. Module Architecture (set up from day one)

```
sprint/
├── core-domain/       # pure Kotlin models + business rules, no platform deps
├── core-data/         # repository interfaces + Room impl + sync client interface
├── core-sync/         # event log merge logic — isolated, heavily unit-tested
├── core-ai/           # LLM client, prompt templates, actor-critic orchestration
├── core-testing/      # fake clock, fixture/seed generators, debug-menu logic — debug builds only, never shipped
├── feature-tracker/   # Compose UI: live tracking, session list
├── feature-kanban/    # Compose UI: task board
├── feature-retro/     # Compose UI: weekly retro view
├── androidApp/        # Android entrypoint, UsageStatsManager, widget, FCM
├── desktopApp/         # (Phase 2) Compose Desktop entrypoint, window daemon
└── server/             # Ktor backend, separate deployable, own repo optional
```

Set this structure up **before writing any feature code** — retrofitting modules later is much more painful than starting clean, and clean module boundaries are exactly what reads well on GitHub.

---

## PHASE 0 — Foundation (Week 1)

**Goal:** Repo scaffolded, builds green, CI running, before any real feature exists.

- [ ] Init KMP project (Android + shared common target; desktop target added later in Phase 2)
- [ ] Set up `core-domain` module with empty data classes: `Task`, `Session`, `Context`, `Retro`
- [ ] Set up `core-data` with Room database (Android target only for now)
- [ ] GitHub Actions CI: build + unit test on every push (this alone fixes the "no CI" gap in your current portfolio)
- [ ] Add `detekt` or `ktlint` for static analysis — cheap, signals code quality
- [ ] Simple local feature-flag system (e.g. a config object gating Phase 12 physical-tracking code paths) — lets you ship the Android MVP without half-built location code interfering
- [ ] Start an `ENGINEERING_DECISIONS.md` file now, add one entry per notable tradeoff as you make it (e.g. "why last-write-wins over CRDTs") — much easier to write in the moment than reconstruct later
- [ ] Write the README skeleton now, not at the end — architecture diagram, module table, "why this exists" section, tech stack badges. Update it as you go so it never falls stale.

### 0a. Dev Testing Harness (build alongside the foundation, not after)

This is what lets you test a day/week of usage — or the weekly retro, rule decay, sync convergence — in seconds instead of actually living through it. Load-bearing for iteration speed on everything after Phase 1; retrofitting it later is expensive because it means threading a `Clock` abstraction through code that didn't originally have one.

- [ ] **Injectable `Clock` interface** — wrap every `Clock.System.now()` call in business logic behind an interface; production uses the real system clock, debug builds swap in a `FakeClock` that can be advanced manually (`advanceBy(7.days)`). This alone unlocks testing the weekly retro job, rule staleness decay, and vacation-mode triggers without waiting real days
- [ ] **Fixture/seed generator** (`core-testing`): functions producing a realistic fake day/week of `Session` objects — varied apps, durations, gaps, weekday/weekend patterns — writable straight into local Room. Populates the UI (widget, Kanban, retro) before you've ever really used the app
- [ ] **Hidden debug menu** (debug builds only): buttons to simulate a day/week of usage, inject a synthetic app-switch or geofence event, force-run the weekly retro job immediately, and fast-forward the rule table by N weeks to test decay
- [ ] **Record-and-replay fixture:** once you have one real day of usage, export it as a JSON fixture; replay it into a fresh install for regression testing going forward — more realistic than synthetic data since it captures your actual messy patterns (ambiguous sessions, rapid app-bouncing), and becomes the seed for the Phase 13 accuracy eval set

**Exit criteria:** Can generate a fake week, force-run the retro job against it, and see a populated retro screen — all without the app having run for a single real day.

**Exit criteria:** Empty app installs on a device, CI badge is green.

---

## PHASE 1 — Core Domain & Local Data (Week 1–2)

**Goal:** Data model finalized, persisted locally, fully unit-tested — no UI yet.

### Data model

```kotlin
data class Context(
    val id: String,           // UUID
    val name: String,         // "Internship", "CSE3443", "Side Projects", "Life"
    val colorHex: String,
    val isActive: Boolean = true   // soft-delete flag — never hard-delete, see 1.1 Context Lifecycle
)

data class Project(               // optional shallow sub-grouping, NOT a nested context
    val id: String,
    val contextId: String,        // e.g. "Side Projects"
    val name: String,             // "Sprint", "Vulgaris", "Corvus"
    val colorHex: String? = null  // inherits context color if null
)

data class Task(
    val id: String,
    val contextId: String,
    val projectId: String?,   // optional link to a Project within the context
    val title: String,
    val status: TaskStatus,   // BACKLOG, IN_PROGRESS, REVIEW, DONE
    val estimatePoints: Int?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deviceId: String      // for last-write-wins tiebreak later
)

enum class SessionSource { APP_USAGE, WINDOW_USAGE, LOCATION, ACTIVITY, MANUAL }

data class Session(
    val id: String,
    val deviceId: String,
    val source: SessionSource,       // set now even though only APP_USAGE is used until Phase 12
    val rawLabel: String,            // package name / window title / geofence name / activity type
    val startTime: Instant,
    val endTime: Instant?,    // null while active
    val contextId: String?,   // null until classified — classifier only ever picks a Context, never a Project
    val projectId: String?,   // optional, assigned after classification (manual, or inferred from window title as a stretch goal)
    val classificationConfidence: Float?,
    val isManuallyCorrected: Boolean
)

data class RetroEntry(
    val id: String,
    val weekOf: LocalDate,
    val summaryText: String,
    val flaggedContextId: String?,
    val generatedByModel: String,
    val promptVersion: String,     // lets you correlate retro quality with prompt iterations later
    val criticApproved: Boolean
)
```

**Data minimization note:** `rawLabel` on `Session` is the most sensitive field (literal app/window names). Plan to null it out once a session is classified and older than N days, retaining only `contextId` + duration — reduces exposure if a device is lost. Build this as a scheduled cleanup job, not a manual step.

### 1.1 Context Lifecycle

Contexts are **dynamic, not fixed** — the 4 seeded at onboarding are just a starting point, editable/expandable anytime from Settings. Everything downstream (Kanban tabs, widget, rule table, classifier prompt, retro aggregation) reads the `Context` table live, never a hardcoded list.

- [ ] Classifier prompt always pulls the *current* active context list at call time — a newly-added context is invisible to the model until this is wired correctly
- [ ] New contexts start with zero rule-table entries — sessions related to them route through the LLM path until enough corrections seed rules for them
- [ ] **Soft-delete only** — "deleting" a context sets `isActive = false` and hides it from pickers/UI, but never removes the row or reassigns historical `contextId` references. Hard-deleting would corrupt past retros and the Phase 13 accuracy eval set.
- [ ] **Guardrail:** block deleting the last active context — the app must always have at least one place to put a session
- [ ] `Context` (and `Project`) rows sync as event-log entries the same as Sessions/Tasks (Phase 8) — without this, phone and laptop can silently diverge on what contexts even exist

### 1.2 Project — a shallow sub-grouping, not a nested context

`Project` gives you "which side project" granularity without making the classifier's job harder:

- [ ] Classifier only ever outputs a `Context` (flat, small option list — stays reliable)
- [ ] `Project` assignment happens afterward: manually via Kanban, or as a stretch goal, inferred from window title/folder name (e.g. an IDE window titled `vulgaris/` auto-suggests the `Vulgaris` project) and surfaced as a low-priority review-card suggestion, never auto-committed silently
- [ ] Retro stays context-level by default; project breakdown is an optional drill-down, not part of the headline insight

### Tasks

- [ ] Implement Room entities + DAOs mirroring the above
- [ ] Repository interfaces in `core-domain`, implementations in `core-data`
- [ ] Unit tests for repository CRUD (use in-memory Room DB for tests)
- [ ] Seed 4 default contexts on first launch

**Exit criteria:** Can create/read/update tasks and sessions purely via unit tests, no UI required to verify correctness.

---

## PHASE 2 — Android Automatic Tracking (Week 2–3)

**Goal:** The app passively logs foreground app usage without you touching it.

- [ ] Request `PACKAGE_USAGE_STATS` permission (user must grant manually in Settings — build the guided permission flow, since this is the #1 place these apps lose users)
- [ ] Foreground service polling `UsageStatsManager.queryEvents()` at a reasonable interval (e.g. every 60s) to detect app switches — **not** continuous polling, to preserve battery
- [ ] Adaptive polling: back off interval when no switches detected for a while, tighten it during active use — helps survive aggressive OEM battery management (Samsung/OnePlus) that can kill fixed-interval services unpredictably
- [ ] Hidden "tracking health" diagnostic screen in Settings (last-seen timestamp, service uptime) — makes gaps caused by Doze/OEM killing debuggable instead of silently missing data
- [ ] On app switch: close previous `Session`, open new one
- [ ] Idle detection: screen-off broadcast receiver auto-closes the active session
- [ ] Write raw sessions to local Room DB immediately (classification happens later, async — never block capture on AI)
- [ ] Detect `PACKAGE_USAGE_STATS` revocation at runtime (user can revoke anytime in Settings) and surface a clear "tracking paused" state — never fail silently

**Exit criteria:** Leave phone running for a full day; verify session table has accurate, gapless app-switch records with no manual input.

---

## PHASE 3 — Classification Engine (Week 3–4)

**Goal:** Raw sessions become tagged contexts automatically, with an actor-critic safety net.

### 3a. Cold-start onboarding (before automatic tracking even begins)

- [ ] Short onboarding flow: pre-seed 5–10 obvious rules manually ("VS Code always → Internship or Study, which?") before automatic tracking starts — otherwise day one hits the LLM at low confidence for everything, which is exactly when your patience for the review queue is lowest

### 3b. Raw signal cleanup (before anything hits the classifier)

- [ ] Debounce app switches under ~5–10 seconds — ignore notification glances, don't create a session for them
- [ ] Merge adjacent same-app sessions separated by a short gap (e.g. IDE → Slack → IDE within 2 min becomes one session)

### 3c. Rule-based pre-filter (cheap, deterministic, first pass)

- [ ] Local lookup table: `packageName/windowTitlePattern → contextId`, seeded with your known recurring apps (VS Code, Obsidian, browser dev tools, etc.) and optionally time-of-day-conditioned rules
- [ ] Only sessions that miss the rule table get escalated to the LLM — cuts LLM calls and variance for the majority of your actual usage

### 3d. LLM classification (actor-critic, for anything the rule table can't resolve)

- [ ] `core-ai` module: LLM client wrapping Groq/Gemini Flash (reuse your Corvus/Falco Ktor HTTP client pattern)
- [ ] **Actor prompt**: given app package/window title + time-of-day + day-of-week + your last N *confirmed* classifications for that app as few-shot context, output `{contextId, confidence}`
- [ ] **Critic prompt**: given the actor's output, sanity-check against the rule-table priors and either approve or flag low-confidence
- [ ] For desktop window titles specifically (messier than package names): embed the title, compare against a small cache of previously-confirmed title embeddings, fall back to the LLM only if nothing's close enough
- [ ] Confidence threshold logic: `> 0.85` auto-commit, `0.5–0.85` queue for end-of-day review card, `< 0.5` always queue
- [ ] Batch classification job runs every few hours, not per-session (cost control — you don't need real-time tagging)
- [ ] Hard daily cap on LLM calls (the rule-filter should absorb most volume) so a classification bug can't quietly rack up API costs unattended
- [ ] **Cap fallback (dead-man's switch):** when the daily cap is hit, don't leave sessions pending indefinitely — fall back to a cheap heuristic (time-of-day rule, or most-frequent-context-for-this-app-recently) so the review queue never balloons past reach
- [ ] Sanitize/escape raw labels (app titles, task titles) before inserting into prompts — low real risk for a personal app, but worth doing as good practice and demonstrable in the writeup
- [ ] Build the review-card UI: swipe-to-confirm for queued sessions, plus a "decide later" snooze option (with a daily cap) so you're never forced into a rushed answer

### 3e. Feedback loop (corrections make the system better, not just fix one row)

- [ ] Every swipe-confirm or retag immediately updates the rule table / few-shot cache for that app pattern — repeated corrections should converge toward an automatic rule
- [ ] **Rule staleness handling:** rules aren't permanent — your own usage isn't stationary (e.g. Chrome for coursework this month, internship next month). Stamp each rule with `lastConfirmedAt`; if a rule hasn't been reinforced by a matching correction in N weeks, drop its confidence and let it re-route through the LLM path rather than trusting it forever
- [ ] Track a rolling accuracy metric: `auto-committed sessions later manually corrected ÷ total auto-committed` — use this to retune the confidence threshold objectively instead of by feel

### 3f. Classification Calibration Week (no new features)

- [ ] A dedicated week between the classifier build and Kanban work: just live with the review queue daily, tune the confidence threshold against the real accuracy metric, harden/prune the rule table before building anything else on top of it. Classification accuracy reliably looks solved against test cases and then needs real-usage tuning — budget for that explicitly rather than assuming Phase 3 code is "done" once it passes tests. Use the debug menu's synthetic app-switch injection (Phase 0a) to stress-test edge cases you haven't personally hit yet, alongside real daily usage.

**Exit criteria:** After a normal day of usage, >80% of sessions are auto-classified correctly with zero manual input; the remainder surface as a short review queue, not a wall of unlabeled data.

---

## PHASE 4 — Kanban & Manual Tasks (Week 4–5)

**Goal:** The mutable, interactive half of the app — task management layered on top of passive tracking.

- [ ] Compose UI: per-context Kanban board (Backlog → In Progress → Review → Done), drag-and-drop or tap-to-advance
- [ ] Manual task creation with optional estimate points
- [ ] Link sessions to tasks optionally (if you want finer-grained tracking than just context-level)
- [ ] Sprint concept: define a date range (matches your actual internship sprint cadence), see burndown per context
- [ ] Optional `Project` filter chip row within a context tab (e.g. Sprint / Vulgaris / Corvus under "Side Projects") — filters the board, doesn't add a new nav level
- [ ] **Plumbing now, feature later:** start capturing window title/folder-name patterns as part of `rawLabel` from Phase 2 onward even though nothing reads them yet — the data is nearly free to capture but expensive to backfill. The actual auto-suggest-a-Project inference feature stays post-MVP so it doesn't add complexity to the classifier while accuracy is still being calibrated (Phase 3f)
- [ ] Optional secondary context tag for edge cases where a session genuinely spans two contexts (e.g. personal debugging during a work lunch) — don't force a single bad classification when the truth is a split

**Exit criteria:** You can run one real sprint (e.g. a week of exam prep) tracked entirely through the board, no external to-do app needed.

---

## PHASE 5 — Weekly Retro (Week 5–6)

**Goal:** The AI feature that makes this more than a tracker — a genuine weekly insight.

- [ ] Scheduled job (WorkManager) runs every Sunday evening
- [ ] Aggregate week's sessions + tasks per context
- [ ] Actor agent generates retro text: time distribution, most context-switched day, one flagged concern (e.g. sustained overload, or a context getting zero attention)
- [ ] Critic agent double-checks the actor's claims against raw aggregates before anything is shown — reject or regenerate if the critic finds a factual mismatch
- [ ] Stamp each `RetroEntry` with `generatedByModel` + `promptVersion` — lets you correlate retro quality with specific prompt iterations later
- [ ] Keep the LLM provider swappable (same multi-provider routing idea as Vulgaris) so a free-tier change doesn't require touching retro logic
- [ ] Local notification when retro is ready
- [ ] Retro screen: short summary, not a wall of text — one headline insight + supporting numbers

**Exit criteria:** First real weekly retro generated from a full week of your own actual data, and it says something true and useful, not generic filler.

---

## PHASE 6 — Glanceable UX & Widget (Week 6)

**Goal:** Make this something you check in 3 seconds, not something you "open."

- [ ] Home-screen widget (Jetpack Glance): today's time-per-context bar, live-updating
- [ ] Widget shows **confirmed classifications only**; sessions still pending review render as a small distinct "unclassified" segment rather than being silently omitted (avoids a misleadingly clean-looking bar while the review queue lags behind batch classification)
- [ ] Notification-shade quick glance (optional): current active context + elapsed time
- [ ] App itself becomes secondary — most days you interact with the widget only, opening the app just for the weekly retro or occasional corrections

**Exit criteria:** You go a full week checking only the widget + the Sunday retro notification, never opening the app cold.

### 6a. Low-touch / Vacation Mode

The system's accuracy depends on you actually swiping cards and touching the Kanban board — that won't hold during a crunch week. Build graceful degradation now rather than letting an ignored backlog become demoralizing:

- [ ] One-tap toggle (Settings or a Home screen banner after N days of an unaddressed queue): pauses the review queue, auto-commits everything at a lower confidence threshold instead of queuing, skips that week's retro rather than generating one from sparse/gappy data
- [ ] Auto-suggest enabling it: if the review queue backlog exceeds a threshold (e.g. 15+ pending) for multiple days, surface a gentle one-time prompt rather than letting the badge count climb indefinitely
- [ ] Exiting the mode doesn't dump the whole backlog on you at once — resume normal queuing going forward, leave the gap period as auto-committed rather than retroactively demanding review

---

## PHASE 7 — Testing, Polish, CI Hardening (Week 7)

- [ ] Unit test coverage target for `core-domain` and `core-sync`-adjacent logic (even pre-sync, structure it testably)
- [ ] Instrumented tests for Room DAOs
- [ ] Golden-file tests for the classifier: a fixed set of (input → expected classification) pairs run in CI, catching silent regressions when prompts change
- [ ] CI runs full suite on PR, not just build
- [ ] README updated with screenshots, architecture diagram, and an honest "known limitations" section (this reads far better to reviewers than pretending it's flawless)

---

## ✅ MILESTONE: Android-only MVP Complete (~8–9 weeks, part-time alongside internship/coursework)

At this point you have a **fully usable single-device app** — this is a legitimate stopping point and a shippable portfolio entry on its own. Everything below is the multi-device expansion; don't start it until the Android app has run on your real daily usage for at least a couple of weeks and you trust its classification accuracy.

---

## PHASE 8 — Sync Server Foundation (Week 8–9)

**Goal:** A thin Ktor backend that relays events between devices — before adding a second client.

- [ ] Ktor server module, Postgres via Exposed or SQLDelight
- [ ] Event log table: append-only, `(id, deviceId, entityType, entityId, payload, timestamp)`
- [ ] `POST /events` (push new events), `GET /events?since=timestamp` (pull)
- [ ] JWT auth: one token issued per device at pairing time, no OAuth complexity needed for single-user
- [ ] Deploy to Fly.io or Railway (cheap, simple for a personal-scale service)
- [ ] `core-sync` module: client-side merge logic — **conflict strategy differs by entity, not one blanket rule:**
  - `Session`: append-only and immutable after close (only mutates via explicit manual correction) — needs event ordering, not conflict resolution
  - `Task`: mutates often (status changes) — `updatedAt` + `deviceId` last-write-wins is appropriate here
  - `Context`: **highest-risk entity for silent data loss.** A rename on one device racing a soft-delete (`isActive` toggle) on another can resurrect a deleted context or drop a rename under naive last-write-wins. Resolve with field-level merge (not whole-row LWW): `isActive=false` wins over `isActive=true` regardless of timestamp (deletes are sticky), while `name`/`colorHex` still resolve via last-write-wins independently
- [ ] Paginate `GET /events?since=timestamp` from day one — a long offline period will return a large backlog, and retrofitting pagination later is painful
- [ ] Use server-received-time (not device-reported timestamps) as the sync ordering tiebreaker — avoids bugs from clock drift between devices
- [ ] Property-based tests (Kotest) generating random interleavings of events from two devices, asserting the merge always converges — stronger signal than example-based tests alone
- [ ] **Explicit Context merge test case:** simulate a rename-vs-delete race and assert the delete-sticky rule holds — this is the specific scenario most likely to silently corrupt data, so it deserves a named test, not just generic coverage
- [ ] Offline handling: sessions queue locally and classify once back online; verify the batch classification job handles a large backlog gracefully, not just steady-state volume
- [ ] Classifier outage fallback: if the LLM provider is down/rate-limited, sessions fall back to "unclassified, pending" rather than blocking capture or dropping data — capture must never depend on AI availability

**Exit criteria:** Two emulator instances (simulating two devices) both writing sessions independently, both converging to the same state after sync — proven via automated test, not manual checking.

---

## PHASE 9 — Encryption & Backup (Week 9)

- [ ] Client-side encryption of session payloads before they hit the server (AES-GCM, key derived on-device, server only ever sees ciphertext)
- [ ] Key backup/recovery flow (even a simple "write down this recovery phrase" is fine for a personal project)
- [ ] JSON export of full local event log — your own portability/backup story
- [ ] Biometric/PIN gate on opening the app itself (this data is arguably more sensitive than photos — don't rely solely on phone lock)
- [ ] Explicit "wipe my history" flow, local and server-side — build this early rather than as an afterthought, given the app tracks app usage and location

**Exit criteria:** Inspecting the server's Postgres directly shows only ciphertext for session data.

---

## PHASE 10 — Windows Desktop Expansion (Week 10–12)

**Goal:** Second real client, proving the architecture actually generalizes — this is the "multiplatform" payoff.

- [ ] Add `desktopTarget` to the KMP project; `desktopApp` module with Compose Desktop
- [ ] **Active-window tracking daemon (the hard part):**
  - Windows: JNA binding to `GetForegroundWindow` + `GetWindowText`, polled on a background thread
  - Design this behind a `WindowTrackerProvider` interface in `core-data` from day one, so macOS/Linux implementations can be added later without touching shared code
- [ ] Reuse `core-domain`, `core-data`, `core-sync`, `core-ai` entirely unchanged — desktop-specific code should be limited to the tracker daemon + Compose Desktop UI shell
- [ ] Desktop app registers as a second device against the same sync server, pairs via a simple device-linking flow (e.g. show a code on phone, enter on desktop)
- [ ] Desktop-specific UX: system tray icon showing today's context breakdown instead of a widget

**Exit criteria:** Start a coding session on your laptop, phone widget reflects it within the sync interval; classification and retro generation both correctly merge cross-device data with no special-casing per platform.

---

## PHASE 11 — Cross-Device Polish & Real Usage (Week 12+)

- [ ] Run both devices for real for 2+ weeks, fix classification edge cases specific to desktop window titles (much noisier than Android package names)
- [ ] Tune the actor prompt with desktop-specific few-shot examples
- [ ] Write the final README pass: architecture diagram covering both platforms, a short GIF/screenshot of the widget + retro + Kanban, and the engineering write-up (event-log sync design, actor-critic classification) — this write-up is what actually gets read by anyone evaluating the repo

---

## PHASE 12 — Physical Signal Tracking (Optional, post-MVP)

**Go/no-go checkpoint before starting:** this is the phase most likely to violate the "well under a minute total" design constraint from §0.1 — physical signals are noisier than app usage and need more confirmation friction per the always-queue rule below. Before building it, honestly assess whether "Gym → Workout" auto-detection is worth the added friction versus just manually creating a workout task in 5 seconds. If the answer is unclear, skip this phase entirely rather than building it half-heartedly.

**Goal:** Extend beyond digital tracking to location/activity — additive to the pipeline, not a rewrite, because `SessionSource` was already built into the model in Phase 1.

**Scope note:** Deliberately excludes Health Connect (workout detail) and shopping/transaction parsing — geofence + activity gives a reasonable proxy for both without the added integration/privacy surface.

- [ ] Geofencing API: define 3–5 fixed zones (Office, Home, Gym, etc.), enter/exit callbacks only — no continuous GPS polling
- [ ] Dwell-time threshold (e.g. 3+ min) before a geofence event is even queued, to filter drive-bys
- [ ] Activity Recognition API: detect `WALKING`, `RUNNING`, `IN_VEHICLE`, `STILL`, `ON_BICYCLE`, using the API's own confidence score as an input rather than treating all detections equally
- [ ] Combine step: overlapping Location + Activity sessions merge into a single inferred session (e.g. Gym zone + Running → "Workout")
- [ ] Physical sessions **never auto-commit** — always queued for swipe-confirm, regardless of confidence (deliberate asymmetry vs. digital sessions, since a wrong geofence trigger is more disruptive than a mistagged browser tab)
- [ ] Feed confirmed physical sessions into the same weekly retro aggregation — retro agent doesn't need to know the source, only the resulting `contextId`

**Exit criteria:** A real gym trip and a real commute both show up correctly in the widget and weekly retro with zero typing, only a single swipe confirmation each.

---

## PHASE 13 — Accuracy Evaluation Loop (Ongoing, start once ~2 weeks of real data exists)

**Goal:** Turn "does this feel more accurate" into a number you can actually track and improve against.

- [ ] Export a sample of confirmed sessions as a small labeled eval set (JSON, from your own real usage)
- [ ] Before changing the classifier prompt, rule table, or confidence thresholds, run the current pipeline against this eval set and record accuracy
- [ ] Re-run after any classification change, compare — keep a simple before/after log
- [ ] Publish a before/after accuracy table in the README once you have a few iterations — this is a strong, concrete portfolio artifact beyond just "it uses AI"

**Exit criteria:** At least one documented accuracy improvement (e.g. "78% → 91% auto-classification accuracy after adding the rule-based pre-filter") backed by the eval set, not a guess.

---

## PHASE 14 — Portfolio Presentation Pass (Ongoing, finalize once MVP + one expansion phase is done)

**Goal:** Make sure the engineering thinking behind the project is actually visible to someone skimming the repo, not just the working app.

- [ ] Finalize `ENGINEERING_DECISIONS.md` (started in Phase 0): last-write-wins vs. CRDTs, why physical sessions never auto-commit, why a rule-filter exists before the LLM call, offline/outage handling — 1 page, tradeoffs not tutorial
- [ ] Record a short demo GIF of the swipe-confirm review-card flow — the "confirm, don't type" UX is the strongest visual differentiator, worth more than a paragraph
- [ ] Publish the Phase 13 accuracy-over-time chart as an image in the README once a few iterations exist
- [ ] README "known limitations" section — reads better to reviewers than pretending it's flawless
- [ ] Architecture diagram covering both platforms once Windows expansion lands

**Exit criteria:** A reviewer who only reads the README (no code) understands what the app does, why key tradeoffs were made, and can see it working.

---

## Suggested Weekly Time Budget

Given you're doing this alongside an internship and exam prep, treat each phase as **flexible, not fixed** — this is a ~12–14 week part-time timeline assuming a few focused hours per week, not a sprint. Phases 0–7 (Android MVP) are the priority; the desktop expansion (8–11) can slip well past exams without hurting the portfolio value of the Android-only milestone.

| Phase | Focus | Est. Duration |
|---|---|---|
| 0–1 | Foundation + domain model | 1–2 weeks |
| 2–3 | Tracking + classification (incl. 3f calibration week) | 3–4 weeks |
| 4–5 | Kanban + retro | 2 weeks |
| 6–7 | Widget + polish + vacation mode | 2 weeks |
| **MVP checkpoint** | | **~8–9 weeks** |
| 8–9 | Sync server + encryption | 2 weeks |
| 10 | Windows expansion | 2–3 weeks |
| 11 | Polish + real usage | ongoing |
| 12 | Physical signals (optional, go/no-go gated) | 1–2 weeks |
| 13 | Accuracy eval loop | ongoing, alongside any classification work |
| 14 | Portfolio presentation pass | ongoing, finalize after MVP + one expansion |

---

## Tech Stack Reference

| Layer | Choice | Reuses from your existing work |
|---|---|---|
| UI | Kotlin Multiplatform + Compose Multiplatform | Corvus/Falco (Compose) |
| Local DB | Room / SQLDelight | Corvus/Falco (Room) |
| Networking | Ktor client | Corvus/Falco (Ktor) |
| LLM | Groq / Gemini Flash, actor-critic pattern | Corvus/Falco directly |
| Backend | Ktor server + Postgres (Exposed) | New — fills backend gap |
| Android tracking | UsageStatsManager + WorkManager | New |
| Windows tracking | JNA → Win32 API | New — the standout systems piece |
| Widget | Jetpack Glance | New |
| CI | GitHub Actions | New — fills current gap |
| Location (Phase 12) | Geofencing API | New |
| Activity (Phase 12) | Activity Recognition API | New |