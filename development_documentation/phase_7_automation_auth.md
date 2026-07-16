# Phase 7: Automation & Authentication

## Overview
Phase 7 transitioned the Sprint MVP from a manual, local-only utility into a fully automated, production-ready, multi-device ecosystem. We accomplished this by leveraging Android's native `WorkManager` for intelligent background scheduling and integrating Supabase GoTrue Auth with Row Level Security (RLS) to ensure cryptographic data ownership.

## 1. The Automation Engine
Previously, users had to manually click buttons in the Settings screen to trigger AI classification, cloud syncs, and retro generation. We implemented a "Set It & Forget It" architecture in `SprintApplication.kt`.

*   **Technology Used:** Android `WorkManager` via `HiltWorkerFactory`.
*   **Configuration:**
    *   `ClassificationWorker`: Scheduled to run every 6 hours.
    *   `SyncWorker`: Scheduled to run every 4 hours.
    *   `RetroGenerationWorker`: Scheduled to run every 24 hours.
*   **Constraints:** All jobs are gated behind `NetworkType.CONNECTED` (Wi-Fi or Mobile Data) and require the device to not be in a low battery state, ensuring zero battery drain when offline.
*   **Persistence:** We used `ExistingPeriodicWorkPolicy.KEEP` to ensure jobs survive app restarts and device reboots without duplicating.

## 2. Authentication & Data Security
To support a seamless multi-device experience (e.g., phone and tablet), we secured the previously public Supabase REST endpoints.

### A. Row Level Security (RLS)
*   Truncated all existing public MVP data.
*   Added `user_id UUID NOT NULL REFERENCES auth.users(id)` to all tables (`contexts`, `sessions`, `tasks`, `retro_entries`).
*   Enabled RLS and created policies enforcing `(auth.uid() = user_id)` on all `SELECT`, `INSERT`, `UPDATE`, and `DELETE` operations.

### B. Authentication Client (`core-sync`)
*   **`AuthManager.kt`**: Implemented a Singleton service that securely caches the JWT `access_token` and `user_id` inside Android's encrypted `SharedPreferences`.
*   **`SupabaseClient.kt` Modification**: The HTTP client now intercepts all network calls, checking `AuthManager` for a valid token and dynamically appending `Authorization: Bearer <TOKEN>` instead of using the public Anon key.
*   **`SupabaseAuthService.kt`**: Created a dedicated Ktor HTTP client to handle `/auth/v1/signup` and `/auth/v1/token?grant_type=password` endpoints.
    *   *Bug Fix:* Resolved a severe `java.net.UnknownServiceException: CLEARTEXT communication` crash caused by a string interpolation escape bug (`\${client.supabaseUrl}`) which forced Ktor to default to `http://localhost`.
    *   *Bug Fix:* Handled Supabase's edge-case where `Confirm Email` is enabled by gracefully parsing empty responses without crashing the strict JSON parser.

### C. Login UI & Routing
*   **`LoginScreen.kt`**: Built a premium Jetpack Compose authentication interface.
*   **`AuthViewModel.kt`**: Manages UI state, loading spinners, and delegates network calls to the auth service.
*   **`MainActivity.kt`**: Updated the root navigation logic to act as a Route Guard. If `authManager.isLoggedIn()` is false, the user is hard-locked to the `LoginScreen`.
*   **`SettingsScreen.kt`**: Integrated a secure "Log Out" flow that clears `SharedPreferences` and triggers `activity.recreate()` to instantly boot the user back to the login wall.

## Outcome
The Sprint app is now a resilient, offline-first application that automatically maps app usage via AI, silently syncs it to the cloud, handles multi-device conflicts via UUIDs, and cryptographically secures all data behind individual user accounts.
