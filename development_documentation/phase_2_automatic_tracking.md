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
6. **Health Monitoring & UI**:
   - Added a `SettingsScreen` and `SettingsViewModel` exposing "Tracking Health" to make Doze-kills or polling gaps extremely visible to the user instead of failing silently.
   - Built `UsagePermissionScreen` to cleanly onboard the user, explaining the need for the permission, and automatically detecting when they grant it via `LifecycleEventObserver`.
   - Wired the UI into `MainActivity.kt` using `hiltViewModel()`, seamlessly handling the permission guard before launching the main `TrackingService`.

## Troubleshooting: Hilt/KSP Stale Build Cache (Critical)

### Symptom
After adding the Phase 2 Kotlin classes (`TrackingEngine`, `TrackingService`, `ScreenStateReceiver`, `UsageStatsTracker`, `PermissionViewModel`, `SettingsViewModel`), Android Studio builds failed with **59 `cannot find symbol` errors** in KSP-generated Java files:

```
MainActivity_GeneratedInjector.java:16: error: cannot find symbol
  void injectMainActivity(MainActivity mainActivity);
```

Every single Kotlin class referenced from Hilt-generated Java was invisible to the Java compiler. The error was systemic, not limited to one class.

### Misleading Signals
- `./gradlew clean` did **not** fix it — the error persisted after clean + rebuild.
- Android Studio **Invalidate Caches & Restart** did **not** fix it — this only clears IDE caches, not the Gradle build cache.
- `./gradlew clean assembleDebug --no-build-cache --rerun-tasks` from the terminal **did** work, which was a critical clue.

### Root Cause
The Gradle build cache (`org.gradle.caching=true` in `gradle.properties`) contained **stale entries** from builds that ran *before* the Phase 2 Kotlin classes existed.

The failure sequence:
1. `compileDebugKotlin FROM-CACHE` — Gradle restored `.class` files from an old cache entry that **did not include** the new Phase 2 classes.
2. `kspDebugKotlin FROM-CACHE` — KSP restored Hilt-generated Java files that **did** reference the new classes (since KSP's cache key changed correctly).
3. `compileDebugJavaWithJavac` — Cache miss (inputs differ between CLI and Android Studio due to IDE-injected properties like `-Pandroid.injected.invoked.from.ide=true`). The Java compiler ran fresh, tried to compile the generated Hilt Java against the incomplete Kotlin `.class` set → 59 errors.

The key insight: `compileDebugKotlin` and `compileDebugJavaWithJavac` had **different cache key sensitivity**. The Kotlin task matched a stale entry while the Java task got a cache miss and ran fresh against incomplete inputs.

### Resolution
Clearing the Gradle build cache at `~/.gradle/caches/build-cache-1/` and rebuilding from scratch repopulated it with correct entries:

```powershell
Remove-Item -Recurse -Force "$env:USERPROFILE\.gradle\caches\build-cache-1\*"
./gradlew clean :androidApp:assembleDebug
```

### Prevention
If this recurs after adding new `@AndroidEntryPoint`, `@HiltViewModel`, or `@Inject`-annotated Kotlin classes:
1. **First try**: `./gradlew clean :androidApp:assembleDebug --no-build-cache` from terminal.
2. **If that works but Android Studio still fails**: Clear `~/.gradle/caches/build-cache-1/` and rebuild.
3. **Do not trust** Android Studio's "Invalidate Caches" for Gradle-level cache issues.

## Additional Build Fix: `compileSdk = 36` Warning Suppression
AGP 8.4.0 was only tested up to `compileSdk = 34`, producing a noisy warning on every build. Per Google's own build output recommendation, added `android.suppressUnsupportedCompileSdk=36` to `gradle.properties`. The warning was cosmetic — the build always succeeded, but the noise obscured real errors.

## Verification
- Built successfully under AGP 8.4.0 with zero warnings (after suppression).
- `SettingsScreen` successfully binds to the `SessionRepository` flow to emit the last seen tracking timestamp.
- Android 14 Foreground service metadata validations pass.

## Testing
To verify the core logic without requiring real device deployment, unit tests were written for the `TrackingEngine` state machine using `MockK` and `kotlinx-coroutines-test`. 
The `pollAndUpdate` function was made `internal` to allow direct, synchronous testing of the following state transitions:
1. **Empty State**: Ignores empty polling results (idle intervals).
2. **Session Creation**: Successfully creates a new `Session` on the first detected app.
3. **Deduplication**: Safely ignores sequential polls of the exact same app.
4. **Session Closing**: Automatically stamps `endTime` on the previous session when a new app switch is detected.

## Completion Status
The background logic and user-facing permission screens for Phase 2 are completely finished and integrated into the main application flow. The raw data pipeline is fully operational.
