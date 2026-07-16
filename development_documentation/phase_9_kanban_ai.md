# Kanban AI Automation: Implementation Plan

The Sprint application already possesses two powerful pillars: **Background Time Tracking** and **Gemini AI Classification**. By connecting these directly to the Kanban board, we can transform the board from a manual checklist into an autonomous "Digital Twin" of your workflow.

This plan details two core automation features to build next.

## Feature 1: Context-Aware Auto-Progress ("Active State" Sync)
Currently, you have to manually drag a task from `BACKLOG` to `IN_PROGRESS`. We can automate this using the `TrackingEngine`.

**How it works:**
1. When the `TrackingEngine` detects that you are actively using an app (e.g., Android Studio).
2. The AI Classifier categorizes this session under `Context: Internship` and `Project: Sprint`.
3. An `AutomationObserver` (running in the background) notices this state change.
4. It checks the Kanban board. If there are no tasks currently `IN_PROGRESS` for `Internship -> Sprint`, it will automatically grab the top task from your `BACKLOG` for that project and move it to `IN_PROGRESS`.
5. *Result:* Your Kanban board physically updates in real-time as you switch apps, reflecting exactly what you are working on without manual input.

**Implementation Steps:**
- Create an `AutomationObserver` injected into `MainActivity` or running as a Coroutine in the background.
- Combine `SessionRepository.getActiveSessionFlow()` and `TaskRepository.getTasksFlow()`.
- Apply heuristics: If `activeSession.contextId == task.contextId`, transition task state.

## Feature 2: AI "Ghost Work" Catcher (Auto-Task Generation)
Often, you do work that you forgot to write a task for. The Kanban board misses this time, breaking the accuracy of your tracking.

**How it works:**
1. We add an **"AI Generate"** magic button to the Kanban header.
2. When pressed, the `KanbanViewModel` pulls your raw tracking data (e.g., "5 hours in VS Code, 2 hours in Chrome") and your currently completed tasks.
3. It sends a prompt to Gemini: *"Analyze this tracking data and compare it to the completed tasks. Identify any major clusters of work that are missing from the board, and generate concise Task titles for them."*
4. Gemini returns structured data (e.g., JSON) suggesting new tasks.
5. The UI presents a bottom sheet: *"It looks like you worked on 'Database Architecture'. Add to DONE?"*
6. *Result:* You get credit for all your work, even if you forgot to plan it out beforehand.

**Implementation Steps:**
- Add `generateTasksFromTracking(sessions: List<Session>, tasks: List<Task>)` to `GeminiRepository`.
- Update `KanbanScreen` with a UI to display and accept these suggestions.
- Insert accepted tasks directly into the Room database via `TaskRepository`.

## Feature 3: Smart Archiving (Optional)
If a task has been `IN_PROGRESS` for 3+ days, but the `TrackingEngine` shows you haven't opened any apps related to that Context, the AI can flag the task as "Stale" and prompt you to move it back to `BACKLOG` or mark it as blocked.

---

### Phase 9 Roadmap
If you approve this plan, we will execute it in the following order:
1. **Step 1:** Build the `AutomationObserver` for real-time Auto-Progress (Feature 1).
2. **Step 2:** Update `GeminiRepository` with the "Ghost Work" prompt (Feature 2).
3. **Step 3:** Overhaul the `KanbanScreen` UI to show AI Suggestions with our new Alexandria design language.
