# Phase 8: Dynamic Taxonomy & Enhanced Visualization

## Objective
Transition Sprint from a rigid MVP into a highly customizable productivity ecosystem by allowing users to define their own tracking structures and providing deeper visibility into their tracked data.

## Step 1: The Context & Project Manager UI
Currently, Sprint uses 4 hardcoded contexts (Internship, Coursework, Side Projects, Life). We will build a new suite of Settings screens to make this dynamic.
*   **ContextManagerScreen:** A dedicated screen to view all active Contexts. Users can click a floating action button to create a new Context, input a name, and select a color using a beautiful preset color picker.
*   **Project Drill-down:** Tapping on a Context will drill down into a `ProjectManagerScreen` where users can define specific projects under that Context (e.g., creating "Thesis" or "AI Final" under the "Coursework" Context).
*   **Soft Deletion:** Implementing safe deletion so hiding a Context doesn't break historical tracking data.

## Step 2: Enhanced Session Display & Manual Correction
Currently, the `TrackerScreen` just shows a basic list of blocks. We will overhaul this to make it actionable.
*   **Session Detail BottomSheet:** When a user taps on a specific time block in their timeline, a sleek BottomSheet will slide up.
*   **Deep Data Visibility:** The sheet will display the exact `rawLabel` (e.g., "YouTube" or "Android Studio"), the exact down-to-the-second timestamps, and the AI's Confidence Score.
*   **Manual Override:** The sheet will contain dropdowns allowing the user to override the AI and manually reassign the session to a different Context or Project. This trains the AI for the future.

## Step 3: Analytics Integration (Optional)
Once the data can be cleanly categorized, we can build a `StatsScreen` that renders a color-coded pie chart showing exactly what percentage of the user's day was spent in each Context.
