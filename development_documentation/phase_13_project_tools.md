# Phase 13: Project Tools - GitHub, Reference Docs & Secret Manager

## Overview
This phase introduces three coherent extensions to the `Project` concept within Sprint, allowing projects to integrate securely with external services and documentation.

The extensions are built sequentially to ensure dependencies are naturally satisfied (e.g., Secret Manager must precede GitHub integration).

---

## 1. Secret Manager (`:core-security`)
**Goal:** Provide secure, encrypted, per-project credential storage (e.g., GitHub Personal Access Tokens, API keys).

### Architectural Decisions
- **Module:** Introduced `:core-security` to encapsulate all cryptographic operations and credential persistence.
- **Tink & Keystore:** Replaced standard `EncryptedSharedPreferences` with explicit `com.google.crypto.tink.Aead` primitives. The Tink keyset is protected by an Android Keystore `MasterKey`, bypassing known OEM main-thread I/O and keyset corruption bugs.
- **Persistence:** Utilizes `androidx.datastore:datastore-preferences` to store the serialized `Secret` entities. This intentionally decouples secrets from Room (`:core-data`) to guarantee they are never swept into the standard cloud sync event loop.
- **Guardrails:** Explicitly documented `// NEVER LOG` tripwires on decryption methods to avoid leaks in `AppLogger` or logcat.

---

## 2. GitHub Integration (Completed)
**Goal:** Connect local `Project` instances directly to remote GitHub repositories, surfacing open issues and recent commits for rapid task tracking.

### Implementation Details
- **Data Models:** Expanded `Project` and `Task` entities with GitHub relational fields. Added `GithubIssueCacheEntity` and `GithubCommitCacheEntity` to support offline viewing.
- **Room Migrations:** Wrote `MIGRATION_2_3` to alter existing task/project tables, and `MIGRATION_3_4` to introduce the caching tables securely.
- **Worker Sync:** Built `GithubClient` utilizing Ktor in `:core-sync` to securely fetch using the PAT from the Secret Manager. `GithubSyncWorker` aggregates projects, retrieves their PATs from DataStore, polls the API, and hydrates the local Room caches. This worker is registered into `SprintApplication` alongside the AI tasks.

---

## 3. Reference Docs (Completed)
**Goal:** Allow users to link external documents (e.g., Obsidian vaults or markdown files) to a project using Storage Access Framework (SAF) URIs, keeping all context reachable from Sprint.

### Implementation Details
- **Data Model:** Created `ProjectDocument` entity and `ProjectDocumentDao`. Updated `SprintDatabase` with `MIGRATION_4_5`.
- **SAF Permissions:** Created `DocumentSafHelper` in `:core-ui` to handle `takePersistableUriPermission` and launch `ACTION_VIEW` intents without needing a dedicated markdown previewer inside the app.
- **Repository Integration:** Added `ProjectDocumentRepository` to the domain layer and bound its Room implementation.
