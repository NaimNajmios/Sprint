# Phase 6: Cloud Synchronization Engine

## Overview
Phase 6 focused on building a robust, bidirectional cloud synchronization layer to back up local Room database data to a remote Supabase PostgreSQL instance. This ensures that the user's data (Contexts, Sessions, Tasks, and Retro Entries) is never lost and serves as the foundation for future cross-device synchronization and authentication.

## Architecture

The cloud sync architecture relies on a "Local First" strategy. The Room database remains the single source of truth for the UI, providing a fast, offline-first experience. The sync engine runs entirely in the background, resolving data between Room and Supabase.

### 1. `core-sync` Module
A dedicated KMP-ready module was created to handle all remote interactions, enforcing clean architecture boundaries.
*   **`SupabaseClient`**: Configures the Ktor HTTP client with `ContentNegotiation` (Kotlinx Serialization), authorization headers, and logging. It reads `SUPABASE_URL` and `SUPABASE_ANON_KEY` from `local.properties` via `BuildConfig`.
*   **`SupabaseApiService`**: Provides REST endpoints (`GET` and `POST`) to fetch and upsert data to Supabase. It uses the `?on_conflict=id` query parameter for upserts.
*   **`SyncEngine`**: The core orchestrator. It executes a full-snapshot bidirectional sync:
    1.  **Push**: Extracts all data from the local Room database and upserts it to Supabase.
    2.  **Pull**: Fetches all data from Supabase and performs bulk inserts/updates (using Room's `OnConflictStrategy.REPLACE`) into the local database.
*   **DTOs**: Dedicated Data Transfer Objects (e.g., `TaskDto`, `SessionDto`) mapped with `@SerialName` to match Supabase's `snake_case` PostgreSQL schema exactly.

### 2. Background Processing
*   **`SyncWorker`**: An Android `CoroutineWorker` managed by `WorkManager` that delegates to `SyncEngine`. This guarantees that cloud synchronization can complete even if the app is closed or the UI is destroyed.

### 3. Settings UI Integration
*   Added a "Cloud Sync" action card to `SettingsScreen`.
*   `SettingsViewModel` observes the `WorkManager` status to provide live UI feedback (Idle, Running, Success, Failed).
*   Refactored the status enum into a generic `ActionStatus` to unify the UI components across Classification, Retro Generation, and Cloud Sync.

## Supabase Schema Implementation
We established the following PostgreSQL schema in the Supabase remote project to match our local domain models:

```sql
-- Contexts Table
CREATE TABLE contexts (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    color_hex TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc', now()),
    is_active BOOLEAN NOT NULL DEFAULT true
);

-- Sessions Table
CREATE TABLE sessions (
    id UUID PRIMARY KEY,
    package_name TEXT NOT NULL,
    raw_label TEXT NOT NULL,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE,
    context_id UUID REFERENCES contexts(id),
    classification_confidence REAL
);

-- Tasks Table
CREATE TABLE tasks (
    id UUID PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT,
    status TEXT NOT NULL, -- 'TODO', 'IN_PROGRESS', 'DONE'
    context_id UUID REFERENCES contexts(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc', now()),
    completed_at TIMESTAMP WITH TIME ZONE
);

-- Retro Entries Table
CREATE TABLE retro_entries (
    id UUID PRIMARY KEY,
    week_of DATE NOT NULL,
    summary_text TEXT NOT NULL,
    flagged_context_id UUID REFERENCES contexts(id),
    generated_by_model TEXT NOT NULL,
    prompt_version TEXT NOT NULL,
    critic_approved BOOLEAN NOT NULL
);
```

## Critical Fixes and Learnings
1.  **Manifest Permissions**: Explicitly added `<uses-permission android:name="android.permission.INTERNET" />` to the `AndroidManifest.xml`. Ktor network calls fail silently or with `Permission denied` exceptions without this.
2.  **Ktor Base URL Duplication**: Fixed an issue where the base URL contained `/rest/v1/` and Ktor's `defaultRequest` appended it again.
3.  **Groq API Deprecations**: Discovered that `llama3-70b-8192` was decommissioned by Groq. Upgraded all AI calls in `RetroGenerationWorker` and `SessionClassifier` to use the modern, supported `llama-3.1-8b-instant`.
4.  **AI Markdown Stripping**: Fixed a silent failure in `SessionClassifier` where LLaMA 3.1 wrapped its JSON output in markdown blocks (````json ... ````). Implemented an `extractJson` helper and added explicit Logcat error printing for robust AI decoding.
5.  **SharedPreferences Bug**: Resolved an issue in `MainActivity` where the `rememberSaveable` onboarding state was resetting on app restart. Onboarding state is now permanently saved in `SharedPreferences`.

## Future Improvements (Post-MVP)
*   **Delta Sync**: The current engine uses a Full Snapshot approach. This is sufficient for an MVP, but as the database grows, we will need to transition to a Delta Sync architecture using `updated_at` timestamps and soft deletes.
*   **Authentication (Supabase Auth)**: Implement Row Level Security (RLS) in Supabase and user authentication to ensure data is private and bound to specific user accounts.
