# Engineering Decisions

A running log of notable tradeoffs made during Sprint's development. Written in the moment, not reconstructed later.

---

## 001 — Room 2.7.x over Room 3.0 (2026-07-15)

**Context:** Room 3.0.0 just released (July 1, 2026) with full KMP support and a new `androidx.room3` namespace.

**Decision:** Use Room 2.7.1 for the Android MVP.

**Why:**
- Room 3.0 is days old — high risk of undiscovered bugs for a production build
- Room 2.7.x already supports KMP targets we need (Android + JVM desktop)
- Migration from 2.7 → 3.0 is straightforward when needed (coexistence is supported)
- The Android MVP doesn't need JS/WasmJs targets that Room 3.0 uniquely provides

**Revisit when:** Desktop target (Phase 10) is actively developed and if Room 3.0 has had 2+ patch releases.

---

## 002 — Hilt over manual DI / Koin (2026-07-15)

**Context:** Need dependency injection for a multi-module Android project.

**Decision:** Use Hilt with KSP.

**Why:**
- Officially recommended by Google for Android
- Compile-time verification catches DI errors before runtime
- KSP processing is significantly faster than KAPT
- Well-integrated with Compose Navigation and ViewModel
- For the KMP desktop expansion (Phase 10), desktop-specific DI can use Koin or manual DI in `desktopApp` only

---

## 003 — Last-write-wins (with entity-specific nuance) over CRDTs (2026-07-15)

**Context:** Need a sync conflict resolution strategy for multi-device (Phase 8+).

**Decision:** Entity-specific strategy, not one blanket rule:
- `Session`: append-only, immutable after close — event ordering, not conflict resolution
- `Task`: `updatedAt` + `deviceId` last-write-wins (mutates often)
- `Context`: field-level merge — `isActive=false` wins (deletes are sticky), while `name`/`colorHex` use LWW independently

**Why:**
- CRDTs add significant complexity for a single-user, 2-device app
- The rename-vs-delete race on Context is the highest-risk scenario — field-level merge handles it correctly
- Server-received-time (not device-reported) avoids clock-drift bugs

---

## 004 — minSdk 26 over minSdk 24 (2026-07-15)

**Context:** The template defaulted to minSdk 24.

**Decision:** Bumped to minSdk 26 (Android 8.0, Oreo).

**Why:**
- `UsageStatsManager` API improvements in API 26+ are needed for reliable tracking
- Foreground service types require API 26+
- Notification channels (required for tracking notifications) were introduced in API 26
- According to Android distribution data, API 26+ covers >98% of active devices
- No meaningful user base is lost

---

*Add new entries here as tradeoffs arise.*
