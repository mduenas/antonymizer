# AGENTS.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

Antonymizer is a competitive antonym word game (Kotlin Multiplatform, Compose Multiplatform) targeting Android and iOS. Players compete against AI bots in Best-of-5 matches with difficulty-scaled bot AI, a ranking system, leaderboards, and platform-specific monetization.

## Commands

```bash
# Build Android debug APK
./gradlew :composeApp:assembleDebug

# Build Android release APK (requires keystore.properties)
./gradlew :composeApp:assembleRelease

# Run all common + Android tests
./gradlew :composeApp:test

# Compile iOS binary (simulator)
./gradlew :composeApp:iosSimulatorArm64MainBinaries

# Screenshot tests (Roborazzi, in separate :screenshots module)
./gradlew :screenshots:recordRoborazziDebug    # record/update baselines
./gradlew :screenshots:verifyRoborazziDebug    # verify against baselines
./gradlew :screenshots:cleanRecordRoborazziDebug  # clean + re-record

# Docker dev environment (Java 17 + Node 20)
docker-compose build && docker-compose run claude
```

## Architecture

MVVM with Navigation Compose. Single Gradle module `:composeApp` contains all app code. A separate `:screenshots` module handles Roborazzi screenshot tests.

### Source Sets

- `commonMain` — All shared business logic, UI, and navigation. This is where nearly all code lives.
- `androidMain` — Android `expect`/`actual` implementations: `MainActivity`, `SettingsStorage.android.kt`, `AdManagerAndroid`, `BillingManagerAndroid`, `TimeUtil.android.kt`.
- `iosMain` — iOS `expect`/`actual` implementations: `MainViewController`, `SettingsStorage.ios.kt`, `AdManagerIos`, `BillingManagerIos` (stubs, not yet implemented), `TimeUtil.ios.kt`.
- `commonTest` — Shared tests using `kotlin-test`.

### Key Layers (all under `com.markduenas.antonymizer`)

- **`domain/game/`** — Core game logic. `GameEngine` orchestrates matches (Best-of-5 rounds, 10 questions/round, 5s time limit). `GameState` is a sealed class with 8 states (Idle → MatchReady → RoundStarting → QuestionActive → QuestionResult → RoundComplete → MatchComplete). `ScoringEngine` handles points with time bonuses and streak multipliers.
- **`domain/bot/`** — `BotBehavior` implements difficulty-scaled AI with accuracy ranges and response time ranges per difficulty level (EASY/MEDIUM/HARD/EXPERT).
- **`domain/ranking/`** — `RankingSystem` and `LeaderboardManager` handle 7-tier ranking (Bronze through Grandmaster) and leaderboard persistence.
- **`data/`** — `WordRepository` loads `antonym_pairs.json` (3000+ pairs with difficulty levels) from Compose resources. `ScoreRepository` and `SettingsStorage` handle persistence via DataStore Preferences.
- **`monetization/`** — `AdManager` and `BillingManager` are interfaces in `commonMain` with platform `expect`/`actual` implementations. Android uses AdMob + Google Play Billing. iOS uses stub classes.
- **`ui/`** — Compose screens (Home, Game, Results, Leaderboard, Settings) with shared components. Navigation uses type-safe routes via `@Serializable` sealed class `Screen` in `NavGraph.kt`.
- **`App.kt`** — Root composable. Sets up `NavHost` with all 5 screens, wires `AppViewModel` and `MonetizationState`.

### Platform Abstractions (expect/actual)

The following use `expect`/`actual` declarations across platforms:
- `SettingsStorage` — DataStore on Android, NSUserDefaults-backed on iOS
- `AdManager` — AdMob on Android, stubs on iOS
- `BillingManager` — Google Play Billing on Android, stubs on iOS
- `TimeUtil` — Platform-specific time utilities
- `Platform` — Platform name/version info

### Dependency Management

All versions are centralized in `gradle/libs.versions.toml`. Use version catalog references (`libs.versions.*`, `libs.plugins.*`, `libs.libraries.*`) when adding or updating dependencies.

## Game Design Constants

- Best-of-5 rounds, 10 questions per round, 5-second time limit per question
- Scoring: base 100 pts + up to 50 time bonus, streak multipliers up to 3.0x, max 450 pts/question
- Interstitial ad cooldown: 3 minutes
- Hints: max 2 per match, requires rewarded ad to unlock

## Build Configuration

- JVM target: 11
- Android: minSdk 24, targetSdk 36, compileSdk 36
- iOS: iosArm64 + iosSimulatorArm64, static framework named "ComposeApp"
- Release builds use ProGuard (minify + shrink resources) and require `keystore.properties` at project root
- Gradle configuration cache and build caching are enabled
