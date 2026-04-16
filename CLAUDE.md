# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**雾岛 (Fog Island)** is an open-source third-party client for [匿名版 X 岛](https://www.nmbxd.com), supporting Android, iOS, macOS, and desktop (JVM). It is a Kotlin Multiplatform project with Compose Multiplatform UI.

## Build Commands

### Android
```shell
./gradlew :androidApp:assembleDebug
```

### Desktop (JVM)
```shell
./gradlew :desktopApp:run
```

### Apple (iOS/macOS)
First build via Gradle, then open `appleApp/appleApp.xcodeproj` in Xcode and select a target.

### Run All Tests
```shell
./gradlew :shared:allTests
```

### Run a Single Test Class
```shell
./gradlew :shared:testDebugUnitTest --tests "ink.duo3.fogisland.shared.util.NmbRichTextTest"
```

### Verify Android Compilation
```shell
./gradlew :androidApp:compileDebugKotlin
```

## Architecture

### Module Structure

- **`/shared`** — KMP library with all business logic, data models, networking, and storage. Shared across Android, iOS, and Desktop.
- **`/androidApp`** — Android application entry point and Android-specific Compose UI.
- **`/desktopApp`** — Desktop JVM application entry point.
- **`/appleApp`** — SwiftUI host project for iOS and macOS (uses the shared KMP framework). iOS-specific code lives under `appleApp/iOS/`, macOS-specific under `appleApp/macOS/`; everything else is shared between both targets.

### Shared Module Layers (`shared/src/commonMain`)

```
shared/
  network/
    api/NmbApiClient.kt      # Ktor HTTP client wrapping all NMB API calls
    model/                   # DTOs (serialized from JSON responses)
  model/                     # Domain models (app-facing, converted from DTOs)
  repository/
    ForumRepository.kt       # Single repository; bridges API, DB, and preferences
  storage/
    db/
      AppDatabase.kt         # Room database (version 12)
      dao/                   # DAOs: Thread, Post, ForumRefresh, ThreadReadProgress,
                             #        SubscriptionThread, PostingDraft, PostingHistory
      entity/                # Room entities
    preferences/             # DataStore: CookieManager, ForumPreferences, CatalogIndexCache
  util/                      # Pure utility logic with unit tests (NmbRichText, NmbThreadReference,
                             # NmbDisplay, NmbUrl, NmbDateTime, NmbThreadPaging, etc.)
```

### Android UI Layer (`androidApp/src/main`)

```
ui/
  FogIslandApp.kt            # Root composable; navigation (androidx.navigation3), drawer
  ForumScreen.kt             # Forum catalog / thread list
  ThreadDetailScreen.kt      # Thread detail / replies
  PostComposerScreen.kt      # New thread / reply composer
  SearchScreen.kt / SubscriptionScreen.kt / HistoryScreen.kt / ...
  components/
    NmbPostCard.kt           # Reusable post/reply card
    NmbRichTextText.kt       # Rich text renderer
    NmbPostReferencePreview.kt  # Inline reference preview
viewmodel/
  ForumBrowseViewModel.kt    # Single ViewModel backed by ForumRepository
data/
  SettingsRepository.kt      # Android-side settings (wraps shared preferences)
  draft/                     # Draft image management (Android file storage)
```

### Key Data Flow

1. **API** — `NmbApiClient` fetches data from NMB API using Ktor; returns DTOs.
2. **Repository** — `ForumRepository` converts DTOs to domain models, caches in Room DB, and persists settings in DataStore.
3. **ViewModel** — `ForumBrowseViewModel` exposes StateFlow/SharedFlow to the Compose UI.
4. **UI** — Compose screens observe ViewModel state; navigation via `androidx.navigation3`.

### Rich Text

NMB posts contain HTML-like markup. `shared/util/NmbRichText.kt` parses it to an annotated string; `NmbRichTextText.kt` (Android) renders it as `AnnotatedString`. References (`>>No.XXXXX`) are resolved via `NmbThreadReference.kt`. Unit tests live in `shared/src/commonTest/`.

### Image Loading

Android uses **Coil** (with GIF support via `GifDecoder`/`ImageDecoderDecoder`). Image URL/CDN rules are in `shared/util/PostImageRules.kt` and `NmbUrl.kt`.

## Development Priorities (from `.local/android-roadmap.md`)

- **P0**: Main user flows — browsing threads, thread detail, navigation boundaries, shared content components.
- **P1**: Card reuse, repository refactoring, posting flow, search/subscription/history pages, minimal tests.
- **P2**: Page transition animations, shared element transitions, visual polish.

Do not work on P2 items until P0 for the relevant module is complete. Each phase must pass `:androidApp:compileDebugKotlin`.
