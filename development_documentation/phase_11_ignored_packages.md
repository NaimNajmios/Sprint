# Phase 11: Ignored Package Management

## Overview
This phase introduces a robust, user-controllable system for managing ignored packages in the Sprint application. Previously, the `TrackingEngine` relied on a hardcoded list of system apps. Now, this logic is dynamically driven by the database, allowing both automatic filtering of system apps and manual curation by the user.

## Completed Tasks

1. **Database & Repository Layer**
   - Added an `isIgnored: Boolean = false` field to `ClassificationRuleEntity` and its corresponding domain model.
   - Implemented Room database migration (Version 1 to 2) to add the new column.
   - Updated `RuleDao` and `RuleRepository` to support querying (`observeIgnoredRules`) and modifying (`setPackageIgnored`) the ignored status of packages.

2. **Tracking Engine Refactoring**
   - Refactored `TrackingEngine.kt` to query `RuleRepository.getRuleForPackage()` during both live tracking (`pollAndUpdate`) and background historical backfill (`backfillHistoricalData`).
   - Packages flagged as `isIgnored` are now seamlessly dropped, effectively bridging the time gap for accurate session continuation without interrupting the tracking flow.

3. **Automated AI Classification Updates**
   - Updated `ClassificationWorker.kt` to automatically flag identified system applications (using `PackageManager` flags) as `isIgnored = true` instead of assigning them an arbitrary "UNCLASSIFIED" label.

4. **Settings User Interface**
   - Created `IgnoredPackagesScreen` and `IgnoredPackagesViewModel` to allow users to view their ignored apps list.
   - Added a "Manage Ignored Apps" navigation entry in the main `SettingsScreen`.
   - Users can now un-ignore apps directly from the Settings UI.

5. **Quick Ignore (Session Inspector)**
   - Enhanced `TrackerScreen` session inspector bottom sheet with an explicit "Ignore App" action button.
   - When a user quick-ignores an app, the associated historical sessions are automatically deleted from the timeline, and the app is dynamically added to the ignored registry.

## Next Steps
- Verify if further improvements to background activity merging are required when interacting heavily with ignored apps (such as the notification shade or system launchers) for extended periods.
