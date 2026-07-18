pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Sprint"

// ── Android application entrypoint ──
include(":androidApp")

// ── Core modules (pure Kotlin / Android library, no UI) ──
include(":core-domain")
include(":core-data")
include(":core-sync")
include(":core-ai")
include(":core-security")

// ── Feature modules (Compose UI) ──
include(":feature-tracker")
include(":feature-kanban")
include(":feature-retro")
include(":core-ui")