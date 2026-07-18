# Sprint

[![CI](https://github.com/YOUR_USERNAME/Sprint/actions/workflows/ci.yml/badge.svg)](https://github.com/YOUR_USERNAME/Sprint/actions/workflows/ci.yml)

> A context-aware, AI-augmented time & task tracker — Android-first, expanding to Windows via Kotlin Multiplatform.

---

## Why This Exists

Sprint fills real gaps in a productivity tracking workflow:

- **Passive tracking** — logs foreground app usage automatically via `UsageStatsManager`, no manual timers
- **AI classification** — an actor-critic LLM agent pair classifies sessions into contexts (Internship, Coursework, Side Projects, Life) with a rule-based pre-filter for efficiency
- **Personal Kanban** — task board mirroring real Scrum sprints per context
- **Weekly retro** — AI-generated weekly insight, fact-checked by a critic agent before display
- **Multi-device sync** — event-log architecture for phone + desktop convergence (Phase 8+)

The design goal: you interact deliberately with the app for **well under a minute per day** — everything else is passive.

---

## Architecture

```
sprint/
├── core-domain/         # Pure Kotlin models + business rules, no platform deps
├── core-data/           # Repository implementations + Room DB
├── core-sync/           # Event log merge logic — isolated, heavily unit-tested
├── core-ai/             # LLM client, prompt templates, actor-critic orchestration
├── feature-tracker/     # Compose UI: live tracking, session list
├── feature-kanban/      # Compose UI: task board
├── feature-retro/       # Compose UI: weekly retro view
├── androidApp/          # Android entrypoint, UsageStatsManager, widget, services
└── desktopApp/          # (Future) Compose Desktop entrypoint, window daemon
```

### Module Dependency Graph

```
androidApp → feature-* → core-data → core-domain
                        → core-ai  → core-domain
                        → core-sync → core-domain
```

---

## Tech Stack

| Layer | Choice |
|---|---|
| UI | Kotlin + Jetpack Compose (Daily Ledger Design System) |
| DI | Hilt (Dagger) |
| Local DB | Room |
| Networking | Ktor Client |
| LLM | Groq / Gemini Flash (actor-critic pattern) |
| Backend | Ktor Server + Postgres (Phase 8+) |
| Android tracking | UsageStatsManager + WorkManager |
| Widget | Jetpack Glance |
| CI | GitHub Actions |
| Static Analysis | detekt |

---

## Build & Run

### Prerequisites
- Android Studio Ladybug or later
- JDK 17
- Android SDK 35

### Build
```bash
./gradlew assembleDebug
```

### Test
```bash
./gradlew test
```

### Lint & Static Analysis
```bash
./gradlew detekt
./gradlew lint
```

---

## Roadmap

| Phase | Focus | Status |
|---|---|---|
| 0 | Foundation — repo, modules, CI | ✅ |
| 1 | Core domain & local data | ✅ |
| 2 | Android automatic tracking | ✅ |
| 3 | Classification engine (actor-critic AI) | ✅ |
| 4 | Kanban & manual tasks | ✅ |
| 5 | Weekly retro | ✅ |
| 6 | Widget & glanceable UX | ✅ |
| 7 | Testing, Polish & Design Overhaul | ✅ |
| **MVP** | **Android-only milestone** | |
| 8–9 | Sync server + encryption | 🔲 |
| 10–11 | Windows desktop expansion | 🔲 |
| 12 | Physical signal tracking (optional) | 🔲 |

---

## Known Limitations

- Single-device only (sync architecture designed but not yet deployed)
- Review queue for low-confidence classifications not yet built
- Desktop target not yet added to the KMP project

---

## License

TBD
