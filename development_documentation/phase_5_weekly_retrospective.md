# Phase 5: Weekly Retrospective & Analytics

## 1. Phase Objective
Phase 5 extends the Sprint app's analytical capabilities from a single-day view (Phase 4) to a full 7-day retrospective window. It provides users with weekly time distribution, daily activity bar charts, top-app insights, and a historical archive of AI-generated productivity summaries.

## 2. Components Built

### 2.1 "Classify Now" Button (Settings Enhancement)
Before building Phase 5, we addressed a usability gap: users had to wait up to 6 hours (or restart the app) to trigger the `ClassificationWorker`. We added a manual trigger directly in the Settings screen.
*   **`SettingsViewModel`**: Added `triggerClassifyNow()` which enqueues a `OneTimeWorkRequest` for `ClassificationWorker` via `WorkManager`. It observes `WorkInfo` to reactively update a `ClassifyStatus` enum (`Idle`, `Running`, `Success`, `Failed`).
*   **`SettingsScreen`**: Added a new "AI Classification" section at the top of Settings, containing a `ClassifyNowCard` with:
    *   A magic wand icon (`Icons.Rounded.AutoFixHigh`).
    *   Reactive status text that changes based on the worker's state.
    *   A "Run" button that disables and shows a `CircularProgressIndicator` while the AI is classifying.

### 2.2 Retro ViewModel (`RetroViewModel`)
The analytical engine for the retrospective screen.
*   **Data Aggregation**: Queries `SessionRepository.getSessionsBetween()` for the past 7 days. Iterates over all classified sessions to compute:
    *   **Weekly totals per context** (`weeklyPerContext: Map<String, Long>`)
    *   **Daily breakdown** (`dailyBreakdown: List<DailyBreakdown>`) — minutes per context for each day of the week (Mon–Sun).
    *   **Top app** — the single package name with the most cumulative usage across the week.
*   **Retro History**: Observes `RetroRepository.observeRetros()` to display any past AI-generated weekly summaries.

### 2.3 Retro Screen (`RetroScreen`)
A rich, scrollable analytics dashboard.
*   **Hero Stat Cards**: Two side-by-side cards showing "Total Tracked" time (e.g., `12h 30m`) and "Top App" (e.g., `Chrome`).
*   **Animated Weekly Bar Chart**: A custom Compose `Row`-based stacked bar chart showing daily activity. Each day's bar is segmented by context color. Bars animate in with a 1.2-second `tween` on first render.
*   **Context Breakdown**: A list of animated `LinearProgressIndicator` rows, one per context. Each shows the context name, color dot, total minutes, and a proportional progress bar.
*   **AI Insights Archive**: If any `RetroEntry` records exist in the database, they are rendered as dated summary cards (e.g., "Week of 2026-07-14: Great job this week! ...").

### 2.4 Navigation Integration
*   Added a fourth tab to the Bottom Navigation Bar: **Retro** (using `Icons.Default.Assessment`).
*   Wired the `RetroScreen` composable into the `NavHost` at route `"retro"`.
*   Tab order is now: **Dashboard** → **Kanban** → **Retro** → **Settings**.

## 3. Files Created / Modified

| File | Action | Description |
|------|--------|-------------|
| `SettingsViewModel.kt` | Modified | Added `triggerClassifyNow()`, `triggerRetroNow()`, `ClassifyStatus` enum |
| `SettingsScreen.kt` | Modified | Added `ClassifyNowCard`, `ActionCard` (reusable), and `Generate Retro` card |
| `RetroViewModel.kt` | Created | Weekly aggregation logic, daily breakdown, top app |
| `RetroScreen.kt` | Created | Full retro UI with charts, stats, and insight cards |
| `RetroGenerationWorker.kt` | Created | HiltWorker that aggregates weekly stats, calls Groq, saves RetroEntry |
| `RetroViewModelTest.kt` | Created | 5 unit tests covering aggregation, top app, daily breakdown, retro passthrough |
| `MainScreen.kt` | Modified | Added Retro tab + route to navigation |

## 4. Testing & Verification

### Manual Testing
1. **Classify Now**: Navigate to Settings → tap "Run" → observe status change from "AI is classifying…" to "Classification complete ✓". Return to Dashboard and verify previously unclassified sessions now have context labels.
2. **Generate Retro**: Navigate to Settings → tap "Run" on the "Generate Weekly Retro" card → observe status → navigate to the Retro tab and verify the AI insight card appears.
3. **Retro Screen**: Navigate to Retro tab → verify hero stat cards show aggregated weekly totals → verify bar chart renders with context-colored segments → verify context breakdown rows show proportional progress bars.

### Automated Testing (`RetroViewModelTest`)
5 unit tests, all passing:
*   `empty sessions produce zero totals` — Verifies default state when no sessions exist.
*   `sessions are aggregated per context correctly` — Asserts exact minute sums for Work and Life.
*   `top app is identified correctly` — Asserts the package with the highest cumulative usage wins.
*   `daily breakdown has 7 entries` — Asserts Mon–Sun labels always present.
*   `retro entries are passed through to state` — Asserts AI-generated summaries appear in state.

## 5. AI Retro Generation Pipeline

The `RetroGenerationWorker` implements the following pipeline:
1. **Guard**: Checks if a `RetroEntry` already exists for the current week; skips if so.
2. **Aggregate**: Queries all sessions from the past 7 days, computes per-context totals and top-5 apps.
3. **Prompt**: Feeds the aggregated stats into a Groq `llama3-70b-8192` prompt with a "productivity coach" system role.
4. **Save**: Stores the AI-generated plain-text summary as a `RetroEntry` in Room.
5. **Flag**: Identifies the least-used context as the `flaggedContextId` for attention.

The worker can be triggered:
*   **Manually**: Via the "Generate Weekly Retro" button in Settings.
*   **Automatically**: Can be scheduled as a `PeriodicWorkRequest` (e.g., every Sunday at midnight).

## 6. Next Steps
*   **Phase 6 (Cloud Sync)**: Implement the `core-sync` Ktor client to back up Contexts, Tasks, and Sessions to a remote server.
