# Sprint — Design System: "Daily Ledger"

Full rewrite, replacing the prior dark-neomorphism direction. Extracted from a fintech UI reference: a **light-primary base with one dark hero panel**, bold hero numerals, a wave chart, an overlapping white sheet of list rows, and a pill-toggle + FAB nav pattern. Material 3 Expressive stays as the underlying *technical* engine (motion physics, shape morphing APIs) — everything about the visual language above it is new.

---

## 0. The Core Structural Shift

The old system was dark-everywhere with context colors doing heavy lifting throughout the chrome. This reference does the opposite: **mostly light/white surfaces, with exactly one dark panel reserved for the single most important number on a screen**, and one restrained brand accent color carrying almost every action. Context color gets demoted from "fills everything" to "a small dot, same role the reference gives its red/green transaction indicators."

This is a deliberate trade: less visually loud in the common case, more legible where it counts.

---

## 1. Base Theme

| Surface | Light mode (default) | Dark mode | Used for |
|---|---|---|---|
| `surface.base` | Near-white, `#F7F7F5` | Near-black, approx. `#121316` | Default screen background |
| `surface.hero` | Deep navy-indigo, approx. `#161A2C` | Same navy — already dark, barely needs to shift | The one dark panel per screen (see §2) |
| `surface.sheet` | Pure white, `#FFFFFF` | Elevated dark gray, approx. `#1C1D21` | The overlapping list sheet (see §3) |

All values are starting points — treat as approximate, tune by eye. The important structural rule carries over unchanged in both modes: **exactly one hero panel per screen, everything else stays on `surface.base`/`surface.sheet`.** Dark mode doesn't mean "dark everywhere again" — it means `surface.base` and `surface.sheet` both drop down near-black while `surface.hero` stays close to where it already was, so the hero panel's contrast advantage (the whole point of §2) doesn't collapse when the system theme switches.

**Appearance setting:** add a Light / Dark / System row to Settings (Compose `isSystemInDarkTheme()` for the System option) — standard three-way appearance control, not a custom toggle. Every token above needs both variants defined in `Theme.kt` from the start; retrofitting dark-mode tokens after screens are built against light-only values is expensive, so wire this into `core-design` in Phase 0/1 rather than treating it as later polish.

**Brand accent — deliberate swap, not a copy of the reference:** the reference uses blue as its single accent throughout (buttons, FAB, selected pill). Sprint keeps **chartreuse `#C8FF00`** in that exact role instead, unchanged across both light and dark modes — same restrained, single-accent structure, your own brand color. This preserves the personal-brand continuity from the portfolio site that mattered in the original system; only the *structure* (one accent, used everywhere action happens) is what's borrowed.

---

## 2. The Hero Panel

The dark navy block at the top of the reference's dashboard screen — big balance figure, small wave chart beneath it, pill toggle top-right. This becomes Sprint's signature "one important number" moment, reused across screens rather than built once:

| Screen | Hero figure | Chart beneath it |
|---|---|---|
| Home | Today's total tracked time | Today's context-switch rhythm as a wave line (each ripple = a switch) |
| Retro | This week's total | Week-over-week trend line, matching the reference's highlighted-point + tooltip treatment |
| Kanban | Sprint burndown % | Burndown line across the sprint's date range |

- **Pill toggle, top-right of the panel** (white-filled active segment on the dark navy, exactly like "This month / Last month" in the reference): Today/This Week on Home, This Week/Last Week on Retro. This is the same physical component as the `ContextSwitcher` from the prior system — one pill-toggle component, reused for time-range switching here and context-filtering elsewhere (see §6).
- **Hero figure typography:** bold IBM Plex Sans (ExtraBold), large — not the mono font. This is a refinement from the previous system: mono is precise but doesn't carry the same visual weight the reference's numerals have. Plex Sans Bold is from the same type family as Plex Mono, so it still reads as "your" typography, just a heavier sibling reserved for exactly this moment.
- **The wave chart:** thin white line on navy, one highlighted data point with a small floating value label — directly ported from the reference. Keep it genuinely minimal: no axis labels, no legend, just the line and one callout.
- **No Context color inside the hero panel.** Like §1.5 in the old system, this stays the one deliberately neutral, calm surface on the screen.

---

## 3. The Sheet-List Pattern

The white panel that overlaps up into the dark header in the reference, containing the transaction rows — this becomes how Sprint shows any list beneath a hero panel: today's sessions, the retro's supporting breakdown, a sprint's task list.

- **Overlap, not a hard break.** The white sheet's top edge rounds and sits slightly over the hero panel's bottom edge (z-axis layering, not a flat stacked layout) — a small but important visual detail from the reference that makes the two pieces feel like one composition instead of two stacked cards.
- **Drag handle** at the sheet's top-center (small gray pill) — even if it isn't functionally draggable yet, include it now; it's cheap and sets up an actual expand-to-fullscreen gesture later without a redesign.
- **Row anatomy** (directly mapped from the reference's transaction rows):

| Reference element | Sprint equivalent |
|---|---|
| Circular merchant logo | App icon (app-usage sessions) or category glyph (physical sessions, Phase 12) |
| Name, bold black | Session/task title |
| Gray timestamp subtitle | Time range or relative time |
| Amount, right-aligned | Duration, in Plex Mono |
| Green/red dot next to amount | Context-color dot — this is where per-Context color now lives, small and consistent, not a full-row tint |

This single row component covers the Home session list, the Retro drill-down, and Kanban's list-view alternative — one component, three contexts.

---

## 4. Shape System

Two tiers now, not five — the reference reads as confident partly *because* it doesn't vary corner radius much:

| Tier | Corner | Used for |
|---|---|---|
| Large | 24–28dp | Hero panel, sheet top edge, primary buttons/pills, modals |
| Small | 8–10dp | List rows, chips, secondary controls |

Shape morphing (from the old system, still valid) stays reserved for one moment: the Review Card confirm animation. Everything else picks one of these two tiers and stays there — resist adding a third.

---

## 5. Typography — Four Roles

| Role | Font | Used for |
|---|---|---|
| Editorial | DM Serif Display | Only the weekly retro headline sentence |
| Hero | **Inter**, ExtraBold | The one big figure per screen (§2) — see note below |
| Data | IBM Plex Mono | List-row durations, timestamps — precise, small-scale, never the hero figure itself now |
| UI | **Inter** (replacing M3 default) | Everything else — labels, body text, nav |

**On the SF Pro request:** the reference mockup uses SF Pro, Apple's system font — that's also why it renders in an iOS status-bar frame. SF Pro's license restricts it to software running on Apple platforms, so it isn't usable in an Android/Windows Compose app without a licensing problem. **Inter** is the standard open-license substitute for exactly this situation — same geometric, neutral, high-legibility character, widely used specifically because it reads as "SF Pro-like" in cross-platform apps. Swapped in for both the Hero and general UI roles here, replacing IBM Plex Sans (Hero) and the M3 default scale (UI) from the prior draft — Plex Mono stays for the Data role since its monospace precision was never about matching SF Pro in the first place.

---

## 6. Pill Toggle — One Component, Two Jobs

The reference uses the same pill-segmented-control shape in two places (Checking/Savings on white, This month/Last month on navy) — same component, inverted coloring to match its surface. Sprint reuses this identically:

- **On `surface.hero` (dark):** white-filled active segment, used for time-range switching (§2)
- **On `surface.base`/`surface.sheet` (light):** chartreuse-filled active segment, used for the Context filter — this replaces the standalone `ContextSwitcher` spec from the old system with the same visual component just placed differently
- Wires into the same `GlobalContextManager.selectedContextId` already read by `KanbanViewModel`/`TrackerViewModel` — no new state, just a new shared visual component

---

## 7. Bottom Nav + Centered FAB

Carried forward from the reference almost directly: five items, minimal line icons, one solid-filled circular FAB elevated above the bar line, centered.

| Position | Icon | Action |
|---|---|---|
| 1 | Home outline | Home screen |
| 2 | Bar-chart outline | Kanban / sprint view |
| 3 (FAB, elevated, filled `brand.primary`) | Plus | Global quick-add — see §8 |
| 4 | List outline | Retro archive |
| 5 | Person/gear outline | Settings |

Nav bar stays on `surface.sheet` (white), never the hero navy — same "chrome shouldn't compete with the hero panel" principle as before.

---

## 8. New: Full-Sheet Modal Pattern (Quick-Add & Review Confirm)

The reference's "Send to Sandy Fotex" screen — full white modal, big editable figure, a casual note field, one large CTA pill — maps onto two of Sprint's most important interactions better than anything in the old spec:

### Quick-Add Task modal
- Big editable title field, same visual weight as the reference's amount field
- Optional single-line note field beneath (mirrors "Heyo, thanks for lunch!") — casual, optional, not a full description form
- Large pill "Add Task" button, full-width, `brand.primary`
- No numeric keypad needed — Sprint has no numeric entry here, so this component is simply shorter than the reference's

### Review Card — full-sheet variant
This is the more interesting reuse. Map the reference's number pad grid onto a **context-picker grid** instead:
- Header mirrors "Send to Sandy Fotex": app icon + suggested Context name, large
- Where the reference shows the amount being typed, show the session's duration (read-only, Plex Mono)
- Where the numeric keypad sits, show a 3-column grid of Context pill chips — tapping one is the equivalent of "typing" your answer
- Large pill "Confirm" button at the bottom, same as "Send"
- This variant is for when the compact `SwipeCard` (from the original Review Queue spec) needs more room — e.g. more than 2–3 candidate contexts — rather than replacing the swipe pattern entirely

---

## 9. Motion

Global `MotionScheme.expressive()` still applies throughout (unchanged from before). New signature motions specific to this system:

| Moment | Motion |
|---|---|
| Sheet reveal (§3) | Slides up and overlaps the hero panel's bottom edge on screen entry, not a hard cut |
| Pill toggle switch (§6) | The filled segment slides between positions rather than snapping — same spring as before |
| Hero figure change (e.g. Today → This Week) | Numerals count up/down rather than cross-fading — small but matches the reference's sense of a "live" figure |

Reduced-motion fallback rule from the old system still applies unchanged: fall back to `MotionScheme.standard()` and simple fades when the system setting is on.

---

## 10. Platform Caveats (carried forward, still true)

- **Glance widgets** still can't inherit this theme automatically — colors resolve manually, no gradients through RemoteViews, corner radius needs an XML drawable fallback. The widget should render a simplified version of the hero panel concept (dark background, big number, no chart) rather than attempting the full composition.
- **Desktop (Phase 10)** still needs a parity check on any newer M3 Expressive APIs used for shape morphing before depending on them structurally.

---

## 11. Where This Lives in Code

Same `core-design` module structure as before — `Theme.kt` (now with light/dark token pairs per §1, plus the Appearance setting wiring), `Shapes.kt` (two tiers), `Motion.kt`, `Type.kt` (four roles, Inter + Plex Mono + DM Serif Display as the three font families to bundle). One addition: `HeroPanel.kt` and `SheetList.kt` in `core-design` or a shared UI module, since these two composites (§2, §3) are reused across Home, Retro, and Kanban rather than being screen-specific.

**Font bundling note:** Inter and IBM Plex are both open-license (SIL Open Font License) and safe to bundle directly as app assets — add them under `core-design/src/main/res/font/` (or the Compose Multiplatform equivalent for the desktop target) rather than relying on a system font that may not be present on every device.
