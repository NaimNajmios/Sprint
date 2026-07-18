# Phase 10: Monochrome UI Overhaul & Critical Fixes

## Overview
This phase focused on finalizing the transition to a high-contrast, pure Black and White "monochrome" aesthetic, resolving major UI legibility issues, and addressing critical system bugs affecting the core tracking and sync mechanisms.

## UI & Design Overhaul

### 1. High-Contrast Monochrome Migration
*   **Color Scheme Refactoring**: Deprecated the legacy Navy and Chartreuse (`Color.kt`, `Theme.kt`) tokens in favor of pure Black (`#000000`) and White (`#FFFFFF`).
*   **Contrast Bug Fixes**:
    *   Removed hardcoded `surfaceVariant` assignments in `Theme.kt` that caused invisible White-on-White text in Light mode for Settings cards and navigation tabs.
    *   Updated the Kanban Floating Action Button (`KanbanScreen.kt`) to properly use `onPrimary` for its `contentColor`, ensuring the `+` icon is highly visible.
*   **Typography Hierarchy**:
    *   Replaced the stark Monospaced font (`IBMPlexMono`) for standard UI elements (e.g., tabs, headers) in `Type.kt` (`labelSmall`, `labelMedium`) with the highly legible `Inter` font. The mono font remains exclusively reserved for pure data presentation.
*   **Retro Dashboard Refinement**:
    *   Completely revised the Retro screen layout. Minimized excessive padding and reduced the physical footprint of the Weekly Bar Chart.
    *   Transformed the AI Insight text into a clean, subtly elevated `Surface` card, reducing the aggressive `headlineMedium` font to an elegant, italicized `bodyLarge`.
*   **Debug Console Revamp**:
    *   Added horizontal scroll bounds to the filter chip row to prevent awkward text wrapping (e.g., "ERR OR").
    *   Enclosed the raw terminal log list in a padded, rounded dark `Surface` container to simulate an embedded terminal window rather than flooding the entire screen.

### 2. Application Launcher Icon
*   **Vector True-Black Implementation**:
    *   Re-wrote `ic_launcher_background.xml` to remove the subtle radial glow ellipse, matching the requested pure black SVG background.
    *   Configured `ic_launcher.xml` and `ic_launcher_round.xml` in `mipmap-anydpi-v26` to point directly to `@drawable/ic_launcher_foreground` instead of `@mipmap`.
    *   **Fallback Eradication**: Ran a PowerShell script to forcefully purge all legacy `ic_launcher.png` assets from the `mipmap-mdpi` through `xxxhdpi` folders, strictly enforcing the new adaptive vector icons across the Android OS.

## Critical System Fixes

### 1. Foreground Tracking Service Initialization
*   **Issue**: The `TrackingService` would only start immediately after a user completed the first-time Permission/Onboarding flow. On subsequent normal cold-boots, the app bypassed the service launch logic entirely, resulting in the service being dead and the Home screen displaying "No tracking data yet."
*   **Resolution**: Hooked a `LaunchedEffect` listener in `MainActivity.kt` that strictly evaluates if the user is authenticated and has permissions upon app load. If both conditions are met, the background Tracking Service unconditionally launches.

### 2. Ktor Auth & Supabase Token Refresh
*   **Issue**: Supabase access tokens possess a 1-hour lifecycle. Previously, the app cached the `access_token` but discarded the `refresh_token`. Once the hour elapsed, background sync operations failed silently due to `401 Unauthorized` responses. The only workaround was forcing the user to re-login to generate a new 1-hour token.
*   **Resolution**: 
    *   Updated `AuthManager.kt` and `SupabaseAuthService.kt` to actively cache and provide the `refresh_token`.
    *   Installed and configured the Ktor `Auth` plugin within `SupabaseClient.kt`. 
    *   When the Supabase API returns a `401`, Ktor automatically halts the request, hits the `/auth/v1/token?grant_type=refresh_token` endpoint, persists the newly issued tokens to `AuthManager`, and instantly replays the blocked sync request in the background.
