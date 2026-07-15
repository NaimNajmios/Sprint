# Sprint: Phase 1 Core Domain & Local Data

## Objective
Implement local data persistence via Room DB in the `:core-data` module, mirroring the pure Kotlin domain models from `:core-domain`. This fulfills the Clean Architecture boundary where Domain defines interfaces, and Data provides implementations.

## Architectural Decisions
1. **Entities and Mappers**: 
   - Entities (`ContextEntity`, `SessionEntity`, etc.) were created in a single `Entities.kt` file to optimize structure and follow the Simplicity rule.
   - Extension functions (`toDomain()` and `toEntity()`) are used to seamlessly map between Room database entities and the domain models.
2. **DAOs (Data Access Objects)**:
   - Defined in `Daos.kt`, covering all CRUD operations.
   - Used Kotlin Coroutines `Flow` for reactive reading of data lists (e.g. `observeActiveContexts()`).
   - SQLite doesn't natively understand Kotlinx `Instant`. Thus, queries against `startTime` (like fetching sessions for a specific day) are processed by computing the start/end `Instant` of the `LocalDate` and querying `BETWEEN` those two epochs.
3. **Type Converters**:
   - Implemented `Converters.kt` to serialize `kotlinx.datetime.Instant`, `kotlinx.datetime.LocalDate`, and domain enums (`TaskStatus`, `SessionSource`) into primitive types SQLite understands (Strings and Longs).
4. **Database & DI**:
   - Configured `SprintDatabase` and seeded it with 4 default contexts ("Internship", "Coursework", "Side Projects", "Life") using a `RoomDatabase.Callback` executed during DB creation.
   - Hilt DI logic was added in `DatabaseModule.kt` to provide the database singleton, DAOs, and bind the specific Room repository implementations to their `:core-domain` interfaces.

## Verification
- Wrote instrumented unit tests in the `:core-data` module using `RobolectricTestRunner`.
- `ContextRepositoryTest.kt` verifies standard CRUD operations and ensures `softDelete` properly filters out inactive contexts from flow emissions.
- `SessionRepositoryTest.kt` verifies that a `Session` with an `Instant` boundary is stored precisely and retrieved accurately via range checks.
- All tests execute purely in-memory, requiring no manual UI intervention.

## Completion Status
The exit criteria for Phase 1 has been met: We can create, read, and update tasks and sessions via unit tests. The local persistence layer is complete.
