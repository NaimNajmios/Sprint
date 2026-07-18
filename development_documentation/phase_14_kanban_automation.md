# Sprint — Phase 14: Kanban Automation & Internal Markdown Engine

## Goal
To elevate the Kanban board from a static list of tasks into an intelligent, highly automated workspace that synchronizes seamlessly with GitHub and provisions necessary resources contextually. Additionally, to provide a native reading experience for rich project documentation.

## Core Features Implemented

### 1. Internal WebView Markdown Engine
* **The Problem:** External viewers broke the seamless immersion of the app and didn't reliably support Obsidian-specific syntax out of the box.
* **The Solution:** We built `DocumentViewerScreen`, which utilizes a hidden HTML/JS shell inside an `AndroidView(WebView)`.
* **Capabilities:** 
    * Fully supports `marked.js` for robust standard markdown parsing.
    * Injects `mermaid.js` to render live diagrams locally.
    * Includes a custom tokenizer plugin that parses Obsidian's `> [!callout]` syntax and renders them as natively-styled UI elements.

### 2. GitHub Two-Way Sync
* **Inbound (Auto-Import):** The `GithubSyncWorker` now checks the local `TaskRepository` during its background sync. Any GitHub issue that does not possess a corresponding Sprint `Task` is automatically generated and placed into the Kanban `BACKLOG`.
* **Outbound (Auto-Close):** We modified `KanbanViewModel.kt`'s drag-and-drop state modifier. When a user drags a GitHub-linked Task into the `DONE` column, Sprint uses the `SecretRepository` to reveal the project's Personal Access Token (PAT) and fires a `PATCH` request to the GitHub API, officially closing the issue remotely.

### 3. Smart Workspace Provisioning
* **Contextual Awareness:** The `KanbanViewModel` dynamically observes all `IN_PROGRESS` tasks and resolves their parent projects.
* **Active Reference Docs:** By identifying active projects, Sprint queries the `ProjectDocumentRepository` and exposes the relevant `ProjectDocument`s to the UI state.
* **UI Integration:** The `KanbanScreen` now features an "ACTIVE REFERENCE DOCS" horizontal ribbon. When a user moves a task to `IN_PROGRESS`, the associated architecture and reference documents instantly appear at the top of the board for immediate, one-tap access.

## Status
Phase completed. The Sprint Kanban board is now a fully automated, GitHub-synced workspace with native rich-text rendering capabilities.
