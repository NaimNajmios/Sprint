# Phase 4: Kanban Dashboard & Visualization

## 1. Objective
Transform the raw, categorized session and task data into a rich, visual user experience. Provide a global context filter, chronological daily tracking, time aggregation charts, and a Kanban board for task management.

## 2. Architecture & Design

### 2.1 Global Context State
*   Implement a `GlobalContextManager` to hold a singleton `StateFlow<String?>` representing the currently selected `Context` (e.g., "Work", "Life", or `null` for Global).
*   Inject this manager into all UI ViewModels (`TrackerViewModel`, `KanbanViewModel`) so that selecting a context at the top of the app filters both screens simultaneously.

### 2.2 Dashboard UI (TrackerScreen)
*   **Hero Visuals**: Use Jetpack Compose canvas and animations to render lively visual elements (e.g., Animated Pie Chart or rounded Progress Bars) showing today's time distribution.
*   **Timeline**: A scrollable vertical list showing today's app usage sessions chronologically. 
*   **State**: Fetches all `Session` entities for `LocalDate.now()` and maps them.

### 2.3 Kanban UI (KanbanScreen)
*   **Board**: A horizontally scrollable row of columns (`Backlog`, `In Progress`, `Review`, `Done`).
*   **Cards**: Task cards that support visual grouping by context.
*   **Interactions**: Click-to-move (or drag-and-drop if viable in Compose) to transition tasks between statuses.
*   **State**: Fetches all `Task` entities and groups them by `TaskStatus`.

### 2.4 Navigation
*   Implement a standard Bottom Navigation Bar in `MainActivity` using `navigation-compose`.
*   Routes: `Tracker`, `Kanban`, `Retro`, `Settings`.
*   A persistent Top App Bar with a dropdown menu to select the Global Context.
