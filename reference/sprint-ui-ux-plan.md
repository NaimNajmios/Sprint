# Sprint — UI/UX Implementation Plan

Companion to the main implementation plan — covers every screen, component, and state needed to support the features already scoped. Organized by feature area, mapped back to build phases.

---

## 1. Design Principles (hold every screen to these)

1. **Glanceable over interactive** — widget/tray icon should answer "how's my week going" without opening the app.
2. **Confirm, don't type** — every AI-uncertain moment is a swipe/tap decision, never a text field.
3. **One interruption at a time** — never stack multiple review cards or notifications; queue and reveal one at a time.
4. **Context = color, always** — each `Context` has one consistent color used everywhere (widget, Kanban, retro charts, review cards) so recognition is instant without reading labels.
5. **Silence is a valid state** — most screens, most of the time, should have nothing new to show. Don't manufacture engagement (no streaks, no guilt-based nudges, no red badges for the sake of it).

---

## 2. Information Architecture

```
App
├── Home (Today) — default landing screen
│   ├── Live context indicator (what's active right now, if known)
│   ├── Today's time breakdown (mirrors widget)
│   └── Pending review queue entry point (badge only if non-empty)
├── Kanban
│   └── Per-context board (swipeable tabs between contexts)
├── Retro
│   ├── This week
│   └── Archive (past weeks, scrollable list)
├── Review Queue (modal/sheet, not a tab — surfaces only when non-empty)
└── Settings
    ├── Contexts (create/edit/color/rules)
    ├── Tracking (permissions, pause/resume, geofence zones — Phase 12)
    ├── Sync & Devices (Phase 8+)
    ├── Privacy (data retention, wipe history — Phase 9)
    └── About / accuracy stats (Phase 13 chart lives here)
```

**Bottom nav (mobile):** Home · Kanban · Retro · Settings — 4 tabs, no more. Review Queue is never a tab; it's a sheet triggered by badge/notification, reinforcing that it's an interruption, not a destination.

---

## 3. Screen-by-Screen Spec

### 3.1 Onboarding (Phase 3a — cold start)

**Screens, in order:**
1. **Welcome** — one sentence on what the app does, no marketing fluff
2. **Create contexts** — pre-filled with 4 defaults (Internship, Coursework, Side Projects, Life), editable name/color, add more
3. **Seed rules** — show a short list of common apps already installed on the device (top 5-8 by existing usage stats), ask "which context does this belong to?" as single-tap chips, not a form
4. **Permission request** — `PACKAGE_USAGE_STATS`, explained in one sentence before the system dialog (users grant permissions they understand, not ones sprung on them)
5. **Done** — drops straight into Home, tracking already running

**Key UX rule:** total onboarding under 90 seconds. If step 3 has more than 8 apps, cap it — the rest resolve through normal review cards later.

---

### 3.2 Home / Today Screen (Phase 6)

**Layout (top to bottom):**
- Header: today's date, live indicator if a session is currently active ("Tracking: Internship · 42m so far") — small, non-intrusive text, not a big timer
- Horizontal stacked bar: today's time-per-context, proportional widths, context colors
- List below bar: same data as rows (context name, color dot, duration) — bar for glance, list for precision
- Bottom: subtle entry point to Review Queue **only if items are pending** ("3 sessions need a tag →")

**Empty state (day just started, no data yet):** simple "Tracking will appear here as your day goes" — not a blank void, not fake data.

---

### 3.3 Review Queue (Phase 3d, Phase 12)

**Interaction pattern:** full-screen or bottom-sheet card stack, one card visible at a time, Tinder-style swipe.

- **Card anatomy:** raw label (app name / "Gym visit") at top, inferred duration, 2-4 context chips as swipe/tap targets, "snooze" as a small secondary action (not a swipe direction, to avoid accidental snoozes)
- **Swipe right** → confirm top-suggested context. **Tap a chip** → confirm a different one. **Snooze button** → defer, capped at 5/day before forcing a decision
- **Progress indicator:** "2 of 5" — queue should always feel finite and closeable, never infinite-scroll
- **Empty state:** queue screen shouldn't exist as an empty view at all — if there's nothing pending, there's no queue entry point to tap (per IA above)

**Physical session cards (Phase 12)** use the same card component, just with different copy ("45 min at Gym, running detected") — no separate UI system needed.

---

### 3.4 Kanban Board (Phase 4)

**Layout:**
- Horizontal swipeable tabs, one per context (color-coded tab underline)
- Within a context: 4 columns (Backlog · In Progress · Review · Done), horizontally scrollable on mobile, side-by-side on desktop
- Task card: title, estimate points (small badge), tap to expand for description/session-links
- **Add task:** floating action button, single-field quick-add (title only) with a "..." to expand for estimate/description — don't force full detail on every quick capture
- **Split-context tag (edge case, Phase 4):** small secondary chip on a task/session showing the split, accessible via long-press, not cluttering the default view

**Desktop difference:** columns shown side-by-side always (more horizontal space), drag-and-drop instead of tap-to-advance.

---

### 3.5 Weekly Retro (Phase 5)

**Layout:**
- Headline insight as a single sentence, large type, top of screen — this is the one thing that must be readable in 3 seconds
- Supporting bar chart below: this week vs. last week, per context
- Optional secondary insights as a short scrollable list beneath (context-switch count, longest focus streak) — collapsed by default, expand on tap
- **Archive view:** simple reverse-chronological list of past retro headlines, tap to expand full detail

**Critic-rejected state:** if generation fails validation, don't show a broken/generic retro — show "Retro is still processing" and retry silently rather than surfacing an untrustworthy summary.

---

### 3.6 Home Widget (Phase 6) & Desktop Tray (Phase 10)

**Android widget (Glance):**
- Compact size: stacked horizontal bar only, no text
- Medium size: bar + top 2 context durations as text
- Updates on session commit, not on a timer — avoid unnecessary battery use

**Desktop tray icon:**
- Icon itself shows a tiny color-segmented ring for today's breakdown (glanceable without opening menu)
- Click → small popover with today's list, no need to open the full app

---

### 3.7 Settings

- **Contexts:** list with color swatches, tap to rename/recolor, drag to reorder (affects tab order in Kanban)
- **Tracking:** toggle pause/resume, permission status indicator (green/red), Phase 12 geofence zone management (map-based zone picker, simple radius circles)
- **Sync & Devices (Phase 8+):** list of paired devices with last-sync time, "pair new device" flow (QR code or short code between phone/desktop)
- **Privacy (Phase 9):** data retention slider (raw label lifespan before minimization), "wipe my history" with a confirmation step that clearly states it's irreversible
- **About/Accuracy (Phase 13):** simple line chart of classification accuracy over time — this doubles as a demo-worthy screen for your portfolio, worth polishing more than a typical settings page

---

## 4. Shared Component Library (build once, reuse everywhere)

| Component | Used in | Notes |
|---|---|---|
| `ContextChip` | Review cards, Kanban tabs, Settings | Color + name, single source of truth for context visual identity |
| `SwipeCard` | Review Queue | Generic enough to handle both app-session and physical-session copy |
| `ContextBar` | Home, Widget, Retro | Stacked horizontal bar, reusable at any width |
| `TaskCard` | Kanban | Compact/expanded variants |
| `InsightHeadline` | Retro | Large single-sentence component, reused for weekly + future insight types |
| `EmptyState` | Home, Review Queue, Retro Archive | Consistent tone — informative, never guilt-inducing or falsely cheerful |

Building these as standalone, prop-driven Compose components from Phase 4 onward (not copy-pasted per screen) keeps the eventual desktop port (Phase 10) close to free for anything that isn't platform-specific chrome.

---

## 5. UI Build Order (mapped to existing phases)

| Implementation Phase | UI Work |
|---|---|
| Phase 3 (Classification) | Review Queue + SwipeCard component |
| Phase 4 (Kanban) | Kanban board, TaskCard, context tabs |
| Phase 5 (Retro) | Retro screen, InsightHeadline, ContextBar |
| Phase 6 (Widget) | Home screen, Android widget (Glance) |
| Phase 9 (Privacy) | Settings: privacy section, wipe-history flow |
| Phase 10 (Desktop) | Desktop tray icon, side-by-side Kanban layout, popover |
| Phase 12 (Physical) | Geofence zone picker (map UI), physical-session card copy |
| Phase 14 (Presentation) | Polish pass on Accuracy screen for demo/README purposes |

**Note on Onboarding:** build it *last* among early phases, once Home/Review Queue/Settings already exist — onboarding is just a guided first-run path through screens that need to exist anyway, not a separate flow to design in isolation.

---

## 6. Accessibility & Platform Conventions

- Don't rely on color alone for context identity — pair every color with a name/icon, since color-blind users need the same at-a-glance recognition
- Swipe gestures on Review Queue need a tap-based fallback (the chip-tap already covers this — just make sure it's equally prominent, not a hidden alternative)
- Respect system font scaling; the `InsightHeadline` component especially needs to reflow gracefully at large text sizes
- Standard platform conventions: Material 3 on Android, matching desktop conventions (system tray behavior, window chrome) on Windows — don't force one platform's patterns onto the other

