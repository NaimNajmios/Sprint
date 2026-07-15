# Sprint: Phase 2 Android Automatic Tracking

## Objective
Implement passive foreground app tracking by hooking into Android's `UsageStatsManager` and persisting the raw time chunks into the Phase 1 Room database.

## Architectural Decisions
1. **Permissions & Configuration**:
   - `PACKAGE_USAGE_STATS` is requested to track app usage.
   - `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_SPECIAL_USE` are required on Android 14+ to run the polling engine persistently.
   - Built a premium Compose UI (`UsagePermissionScreen`) to onboard users politely and bounce them cleanly into Android Settings.
2. **UsageStatsTracker**:
   - Wraps the `UsageStatsManager` system service.
   - Optimized to only query for `ACTIVITY_RESUMED` events since the *last query time*, preventing expensive redundant data processing.
3. **TrackingEngine**:
   - The central nervous system of Phase 2.
   - **Adaptive Polling**: Polls every 10 seconds when active (to detect switches quickly), but backs off to 60 seconds if no switches occur to preserve battery.
   - Writes directly to the `SessionRepository` when distinct app switches occur, and closes active sessions precisely.
4. **Broadcast Receivers**:
   - `ScreenStateReceiver` detects `ACTION_SCREEN_OFF` to halt polling completely (zero battery drain when idle) and instantly closes the active Session. When the screen wakes up, it resumes tracking.
5. **Foreground Service**:
   - `TrackingService` elevates the engine into a foreground process, making it resistant to aggressive OEM killing (Samsung/OnePlus Doze logic).
6. **Health Monitoring**:
   - Added a `SettingsScreen` and `SettingsViewModel` exposing "Tracking Health" to make Doze-kills or polling gaps extremely visible to the user instead of failing silently.

## Verification
- Built successfully under AGP 8.4.0.
- `SettingsScreen` successfully binds to the `SessionRepository` flow to emit the last seen tracking timestamp.
- Android 14 Foreground service metadata validations pass.

## Completion Status
The background logic and user-facing permission screens for Phase 2 are complete. The raw data pipeline is fully operational.
