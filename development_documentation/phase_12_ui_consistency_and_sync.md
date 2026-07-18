# Phase 12: UI Consistency & Supabase Sync Fixes

## Overview
This phase focused on standardizing the user interface interaction patterns across all main screens and resolving a critical data synchronization bug preventing local tracking data from properly upserting to the cloud backend.

## UI & Design Consistency

### 1. Unified "Pull-Up" Sheet Interaction
*   **Issue**: Previously, only the `TrackerScreen` featured a seamless "pull-up" scrolling mechanic where the list content elegantly slid over and pushed the static `HeroPanel` out of view. `KanbanScreen` and `RetroScreen` relied on static Column layouts with constrained nested scrolling.
*   **Resolution**: 
    *   Refactored both `KanbanScreen` and `RetroScreen` to utilize a root `LazyColumn` layout. 
    *   The `HeroPanel` was moved into the primary `item {}` block of the `LazyColumn`, ensuring it scrolls naturally with the rest of the content.
    *   Applied the consistent `-24.dp` vertical offset and `surface` background rendering to match the "sheet-over-hero" aesthetic established in the main tracker.

### 2. Prominent Unclassified App Call-to-Action
*   **Issue**: Users were not intuitively guided to manually resolve `UNCLASSIFIED` applications (instances where the AI Critic rejected the Actor's classification or the model was uncertain).
*   **Resolution**: 
    *   Updated `TrackerScreen` to dynamically partition the daily session list into two distinct groups: Unclassified and Classified.
    *   Injected a highly visible **"ACTION NEEDED: CLASSIFY APPS"** banner at the top of the unclassified section.
    *   Enhanced the unclassified `SessionCard` visuals by applying a subtle red error-tinted background (`errorContainer`) and a red outline border to explicitly draw user attention and prompt manual context assignment.

## Critical System Fixes

### 1. Supabase PostgREST Upsert Resolution
*   **Issue**: The `SyncWorker` successfully executed, but all tables in Supabase remained completely empty. The system was silently failing during network requests with a `400 Bad Request`.
*   **Resolution**: 
    *   Identified that PostgREST (the API engine beneath Supabase) strictly mandates a `Prefer: resolution=merge-duplicates` header whenever the `on_conflict` query parameter is utilized during a `POST` (upsert) operation.
    *   Modified `SupabaseApiService` to inject the `header("Prefer", "return=representation,resolution=merge-duplicates")` directive across all critical push channels (`upsertContexts`, `upsertTasks`, `upsertSessions`, `upsertRetroEntries`). 
    *   This restored bi-directional sync capability, allowing local Room SQLite data to flawlessly materialize in the cloud without duplicate key conflicts.
