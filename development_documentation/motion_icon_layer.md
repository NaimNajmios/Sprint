# Sprint — Motion, Icon & Delight Layer

A layer on top of the Daily Ledger design system (`sprint-design-system.md`) and UI/UX plan (`sprint-ui-ux-plan.md`) — this doc doesn't redefine tokens or screens, it specifies exactly where and how to add state-of-the-art motion, iconography, and animated delight across the whole app, with real, current (mid-2026) Compose APIs, not aspirational ones.

---

## 0. The Guardrail — Read This Before Anything Else

"Lively" here means **motion craft and visual coherence**, not manufactured engagement. The original Design Principles (§0 of the UI/UX plan) explicitly rule out streaks, guilt-based nudges, and manufactured urgency — that constraint doesn't loosen just because this pass adds Lottie and shared-element transitions. Every item below should make an existing, honest moment feel better-crafted, never invent a reason to open the app. If something in this doc starts nudging you back in for its own sake, cut it.

---

## 1. App Icon

**Concept:** the abstracted wave/sparkline from the `HeroPanel`'s chart (design system §2), frozen into the icon — the icon becomes a tiny echo of the exact visual signature you see inside the app daily, so icon and product read as one continuous system instead of a logo designed in isolation.

A concept preview is included as `sprint-icon-concept.svg` — navy `surface.hero` background, chartreuse wave glyph, with the same highlighted-data-point dot treatment used in the in-app chart.

**For actual Android adaptive icon implementation**, split into the standard two layers:
- **Background layer:** flat `surface.hero` navy (`#161A2C`), no glyph — matches `ic_launcher_background.xml`
- **Foreground layer:** the wave path + dot only, transparent background, kept inside the standard adaptive-icon safe zone (inner ~66% of the canvas) so it isn't clipped on circular/squircle OEM masks — matches `ic_launcher_foreground.xml`
- **Monochrome layer (Android 13+ themed icons):** the wave path alone, single-color, no chartreuse — lets the icon tint to match the user's system wallpaper/theme when Themed Icons is enabled at the OS level. Cheap to add once you have the foreground vector, and it's a real, current Android capability worth using rather than skipping.

**Icon shortcuts (long-press on home screen):** wire 2–3 static shortcuts once Phase 4/B4 exist — "Quick Add Task," "Open Review Queue" (only shown if non-empty, if the shortcuts API allows conditional visibility in your target SDK), "This Week's Retro." Small, cheap, genuinely useful.

---

## 2. Iconography System

Default Material Icons read as generic against a custom design system this considered — worth a coherent icon set instead:

- **Direction:** a Phosphor- or Lucide-style outline set (light, consistent stroke weight, geometric, rounded terminals) — matches the two-tier shape system's restraint (design system §4) better than Material's filled default style
- **Duality convention:** outline for unselected nav states, filled (or outline-plus-fill-accent) for selected — standard, legible pattern, cheap to implement once the base set is chosen
- **Stroke weight** should visually match the wave glyph in the app icon (§1) — roughly 2dp at typical UI sizes — so icon-to-icon consistency holds from launcher through every screen
- **Sourcing note:** verify exact package/licensing availability for whichever set you pick at build time — icon library ecosystems for Compose Multiplatform move fast enough that specifics here would go stale between writing and building

---

## 3. Lottie — Where It Actually Earns Its Place

`com.airbnb.android:lottie-compose` is the real, long-stable library for this — no ambiguity there. The discipline is restraint: Lottie for moments that are genuinely rare or genuinely emotional, never for routine UI chrome (a routine button press should stay a Compose animation, not a Lottie file).

| Moment | Why Lottie, specifically |
|---|---|
| Onboarding illustrations (Phase 3a) | A short, one-time sequence — worth hand-crafted motion since you'll build it once and every new install sees it |
| Permission request explainer | Turns "why does this need Usage Access" into a 3-second animated explanation instead of a wall of text |
| Empty states (Home, Review Queue never-populated, Retro Archive) | A small looping illustration beats static copy — but keep it subtle, not cartoonish, matching the app's restrained tone |
| Retro-ready moment | One small, tasteful animated cue when you open a freshly-generated retro — not confetti, not celebratory in a gamified sense, more like a gentle "this is ready" reveal. This is the one place to be most careful against the §0 guardrail |
| Sync-in-progress (Phase 8+) | A small looping indicator, replacing a generic spinner |

**Everything else** — button presses, pill toggle switches, card confirms — stays native Compose animation (`animateFloatAsState`, `spring()`), not Lottie. Lottie is for the handful of moments listed above, not a general-purpose animation solution.

---

## 4. Shared Element Transitions — Now Stable, Worth Using

`SharedTransitionLayout`/`SharedTransitionScope` (androidx.compose.animation) graduated out of experimental/alpha status as of the Compose 1.10/1.11 line in 2026 — safe to build against directly now rather than pinning alpha dependencies, which was the caveat this API carried as recently as a couple years ago.

**Concrete applications:**
- **Review Queue → Full-sheet variant (UI/UX plan §3.3):** the compact `SwipeCard`'s icon/label should visually morph into the full-sheet header, not cut between two unrelated layouts
- **Kanban `TaskCard` → expanded detail:** tapping a card should feel like the card *becoming* the detail view, not a navigation push
- **Retro headline → Archive detail:** tapping a past retro in the archive list shares bounds with the summary line it expands into

**Known limitation to design around (from Google's own docs):** shape clipping isn't automatically animated between shared elements — so the Review Card's square-to-circle confirm morph (design system §4) should stay implemented as a standalone `Animatable`-driven corner-radius animation (as originally speced), not folded into the shared-element system. Use shared elements for *position/size* continuity across screens; keep the shape-morph technique for the *single-composable* confirm moment.

---

## 5. List & Item Motion

`Modifier.animateItem()` on lazy list items automatically animates insertion, removal, and reordering — real, stable, cheap to add:

- **`SheetList` rows** (design system §3): a new session appearing, or a row being removed after Review Queue confirmation, should animate in/out rather than popping
- **Kanban columns:** task reordering within/across columns animates automatically once cards are in a `LazyColumn`/`LazyRow` with stable keys
- **Review Queue stack:** a confirmed card's removal should animate out before the next card is fully visible — small but this is the difference between the queue feeling responsive versus abrupt

**Requirement for all of the above:** stable, unique keys derived from actual domain IDs (`Session.id`, `Task.id`), never list position — position-based keys break exactly this kind of animation on reorder/recomposition.

---

## 6. Haptics

`LocalHapticFeedback.current` — no new dependency, already available. Sparse and purposeful, not on every tap:

- Review Card swipe-confirm (a light, satisfying tick — this is the single highest-value haptic touchpoint in the app, since it's the most repeated confirm action)
- `PillToggle` segment switch
- Kanban task status change
- FAB long-press (reveals secondary actions, per the icon shortcuts idea in §1)

Skip haptics on the FAB's primary tap, nav switches, and anything already carrying strong visual motion — doubling up visual + haptic on low-stakes actions reads as excessive rather than polished.

---

## 7. Predictive Back Gesture

Android 13+'s system predictive-back gesture, supported in Compose via `PredictiveBackHandler` — a genuinely current piece of platform-consistent polish:

- Review Queue full-sheet modal and Quick-Add modal (§3.8 of the UI/UX plan) should both respond to the back gesture with the system's live preview/scale-down effect, not a hard dismiss
- This is cheap once the modals exist as proper Compose destinations — mostly a matter of not fighting the system gesture with a custom back handler that ignores it

---

## 8. Loading States — Skeleton, Not Spinners

Where `HeroPanel`/`SheetList` haven't loaded data yet (cold start, slow sync), use a shimmer/skeleton placeholder shaped like the real content rather than a centered spinner:

- A simple animated gradient sweep (`Brush.linearGradient` + `animateFloat` looping) over gray placeholder shapes matching the hero figure and row layout — a well-known, standard Compose pattern, no special library needed
- This reads as "the real content is a moment away" rather than "something is happening, wait" — a meaningfully different feeling for a screen you check daily

---

## 9. Sound — Optional, Off by Default

A single subtle confirm "tick" sound on Review Card swipe-confirm is worth *offering*, but default it **off**, with a toggle in Settings. This app gets checked during work/study contexts where sound is unwelcome by default — respect the same restraint as the "silence is a valid state" principle rather than assuming sound adds delight universally.

---

## 10. Where This Lives in Code

- `lottie-compose` — new dependency, `feature-onboarding` and wherever empty-states/retro-reveal live
- `androidx.compose.animation` 1.10+/1.11+ — already likely on your BOM if you're current; confirm `SharedTransitionLayout` isn't still gated behind an opt-in on whichever exact version you're pinned to
- Haptics, `animateItem`, predictive back — all already available in stable Compose/Compose Multiplatform, no new dependencies
- Icon assets (`ic_launcher_foreground.xml`, `_background.xml`, monochrome variant) — standard Android adaptive icon resource structure, seeded from `sprint-icon-concept.svg`

---

## 11. Phase Mapping — So This Is Schedulable, Not Just a Mood Board

| Item | Slots into |
|---|---|
| App icon, iconography | Phase 0/1 — do early, it's foundational and cheap |
| Skeleton loading, haptics, `animateItem` | Alongside Phase 4–6 as each screen is built — don't retrofit later |
| Shared element transitions | After Phase 4 (Kanban) and B4 (Review Queue UI) both exist — needs two real screens to connect |
| Lottie (onboarding, permission, empty states) | Phase 3a (onboarding) and Phase 6 (widget/polish) |
| Predictive back | Once Review Queue/Quick-Add modals exist as real destinations |
| Sound toggle | Low priority — last, and only if it still feels worth it after everything else is real |
