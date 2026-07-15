# Phase 3: Classification Engine

## 1. Overview
This phase introduces intelligence and data sanitization to the raw `UsageEvents` logged in Phase 2. The classification engine reduces noise via debouncing/merging, assigns deterministic contexts via local rule-matching, and escalates unknown applications to an LLM Actor-Critic pipeline.

## 2. Completed Milestones

### 2.1 Raw Signal Cleanup (Phase 3b)
*   **Debouncing**: `TrackingEngine` now discards sessions shorter than 10 seconds to prevent tracking accidental app launches or rapid context switches.
*   **Session Merging**: If an app is closed but then reopened within 2 minutes, `TrackingEngine` detects this and merges the two sessions into one continuous block by reopening the previous session.

### 2.2 Rule-Based Pre-filter (Phase 3c)
*   Introduced `ClassificationRule` domain model, Room Entity, DAO, and Repository in the `core-data` module.
*   Wired `RuleRepository` into the `TrackingEngine`. When a session closes, it deterministically assigns a context without incurring any AI cost if a match is found in the local database.

### 2.3 Cold-Start Onboarding (Phase 3a)
*   Developed a Compose UI flow (`OnboardingScreen`) triggered after permissions are granted.
*   Scans the user's OS for their top 5 most used apps over the past 3 days (`UsageStatsTracker.getTopRecentApps`).
*   Prompts the user to manually categorize these top apps to jump-start the Rule Table, drastically reducing the number of LLM inferences needed on Day 1.

### 2.4 LLM Actor-Critic Engine & Background Calibration (Phase 3d & 3f)
*   Created the `core-ai` module utilizing Ktor (OkHttp engine) to connect with LLM providers (e.g., Groq/OpenAI).
*   Built `SessionClassifier.kt` utilizing an **Actor-Critic architecture**:
    *   **Actor**: Predicts the context based on system active contexts and the package name.
    *   **Critic**: (Smarter model) Evaluates the actor's prediction and approves or flags it.
*   Implemented `ClassificationWorker.kt`, a Hilt-injected `CoroutineWorker` scheduled by Android `WorkManager`.
*   Every 6 hours, the Worker retrieves unclassified sessions, runs them through the Actor-Critic pipeline, saves the assigned context, and caches the result into the Rule Table for future deterministic pre-filtering.

## 3. How to Test

### 3.1 Automated Tests
Currently, the core state-machine of the application is covered by unit tests. You can run all tests automatically by executing:
```bash
./gradlew test
```
**What is tested automatically:**
*   **`TrackingEngineTest.kt`**: 
    *   Verifies that a `< 10s` session is properly debounced and dropped.
    *   Verifies that reopening the same package within `< 2m` reopens the old session instead of creating a new one.
    *   Verifies that when a session closes, the engine queries the Rule Table and injects the `contextId` immediately if a rule exists.

### 3.2 Manual Testing Guide
To manually verify the UI and background workers, deploy the app to an emulator or physical device:

1.  **Onboarding UI Test:**
    *   Clear the app data (or perform a fresh install).
    *   Launch the app and grant the "Usage Access" permission.
    *   You should immediately be presented with the "Sprint Setup" Onboarding Screen.
    *   It will list your most-used apps. Assign them to a Context (e.g., "Life" or "Work").
2.  **Tracking & Merging Test:**
    *   With the app running, open an app (e.g., Chrome) for 5 seconds and go back to home. Wait 1 minute. Open Chrome again for 20 seconds.
    *   *Expected behavior:* The first 5-second session is dropped. The 20-second session is logged as a single session.
3.  **Classification Worker Test:**
    *   Since the WorkManager job is scheduled for every 6 hours, it's hard to test by waiting.
    *   You can manually trigger the background worker using Android Studio's **App Inspection -> Background Task Inspector**, select the `ClassificationWorker`, and hit "Execute".
    *   *Expected behavior:* The logs will indicate sessions being sent to the LLM and the Rule Table being updated.
