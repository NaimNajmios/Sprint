# Alexandria Design System Analysis

## North Star: "The Digital Curator"
The objective of the Alexandria design overhaul is to shift Sprint from a standard utility app into a **premium, scholarly reading experience**. It emphasizes dense information clarity through authoritative typography and generous whitespace.

## 1. Color Palette Translation
The app needs a complete `ColorScheme` overhaul in Jetpack Compose:
*   **Primary (`#094cb2`)**: Used strictly for links, primary actions, and focus states.
*   **Tertiary (`#6d5e00`)**: Archival Gold. Used sparingly for highlights, badges, and premium indicators.
*   **Surfaces (`#faf9fa`, `#dbdadb`)**: The app will rely on subtle background color shifts (`surface-container-lowest` to `surface-dim`) rather than using borders.

## 2. Typography Engine
We will need to update the `Typography` in `Theme.kt` and import new Google Fonts (or bundled font resources):
*   **Headlines & Display:** Noto Serif (Large, authoritative, generous leading).
*   **Body Text:** Inter (Modern clarity for dense text).
*   **Labels & Metadata:** Public Sans (Archival metadata feel).

## 3. Elevation and Boundaries (The "No-Line" Rule)
*   **Strict Rule:** No explicit 1px borders. Hierarchy must be established through tonal layering.
*   **Exceptions:** If borders are absolutely necessary, they must be "Ghost Borders" (`outline_variant` at 15% opacity).
*   **Modals & BottomSheets:** Shadows must be extremely diffused (24-40px blur, 4-6% opacity).

## 4. UI Component Overhaul
*   **Cards:** Remove `Card` borders and dividers. Use spacing or alternating `surface` colors.
*   **Buttons:** 
    *   Primary: Gradient fill (Primary to Primary Container).
    *   Secondary: Surface-high background with Primary text.
    *   Tertiary: Text-only with a hover underline.
*   **Shapes:** Completely eliminate sharp corners. All components must have at least a `sm` (small) rounding radius.

## Implementation Plan for Android (Jetpack Compose)
1. **Phase 1: Foundation.** Update `ui/theme/Color.kt`, `ui/theme/Type.kt`, and `ui/theme/Shape.kt` with the Alexandria tokens. Add the new fonts to the `res/font` directory.
2. **Phase 2: Core Components.** Refactor global app bars, bottom navigation, and floating action buttons to use the new surface tonal shifts and drop shadows.
3. **Phase 3: Screen Overhaul.** Systematically update `TrackerScreen`, `KanbanScreen`, and `ContextManagerScreen` to remove hard dividers, applying Noto Serif headlines and the No-Line rule.
