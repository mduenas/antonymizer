# Antonymizer

Competitive antonym word game where players compete against AI bots in Best-of-5 matches. Features difficulty-scaled bot AI, ranking system, leaderboards, and platform-specific monetization.


## Memory

Use the **SimpleMem** MCP (`simplemem` tool) to store and retrieve project memory across sessions.
- Store: decisions, settings, file structure, user preferences, TODOs, constraints, recurring patterns
- Retrieve: query at the start of each session and before making significant decisions
- Tools: `memory_add`, `memory_query`, `memory_retrieve`, `memory_stats`, `memory_clear`
- Only save information that will be helpful across sessions.

## Tech Stack

- **Kotlin**: 2.3.0
- **Compose Multiplatform**: 1.10.0
- **Navigation Compose**: 2.9.0-alpha14
- **DataStore Preferences**: 1.1.2
- **Kotlinx Serialization**: 1.7.0
- **Kotlinx Coroutines**: 1.9.0
- **Kotlinx DateTime**: 0.6.0
- **Google Play Billing**: 7.1.1
- **AdMob**: play-services-ads 24.2.0
- **Android**: minSdk 24, targetSdk 36, compileSdk 36
- **iOS**: arm64, simulatorArm64
- **Testing**: Roborazzi 1.42.0, Robolectric 4.14.1

## Commands

```bash
# Build
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:assembleRelease

# Test
./gradlew :composeApp:test

# Screenshot tests
./gradlew :composeApp:recordScreenshots
./gradlew :composeApp:verifyScreenshots

# iOS
./gradlew :composeApp:iosSimulatorArm64MainBinaries

# Docker development
docker-compose build && docker-compose run claude
```

## Architecture

MVVM with Navigation Compose. DataStore for persistence (Android: PreferencesDataStore, iOS: NSUserDefaults wrapper).

```
composeApp/src/commonMain/kotlin/com/markduenas/antonymizer/
├── domain/
│   ├── game/            # GameEngine, GameState (8-state machine), ScoringEngine
│   ├── bot/             # BotBehavior with difficulty-scaled accuracy/response times
│   └── ranking/         # RankingSystem, LeaderboardManager, RankProgress
├── data/
│   ├── model/           # PlayerStats, Rank, LeaderboardEntry, BotDifficulty, AntonymPair
│   ├── repository/      # WordRepository, ScoreRepository
│   └── source/          # SettingsStorage (expect/actual), WordDataSource
├── ui/
│   ├── screens/         # HomeScreen, GameScreen, ResultsScreen, LeaderboardScreen, SettingsScreen
│   ├── components/      # GameComponents (AnswerButton, TimerBar, WordCard, StreakIndicator)
│   ├── theme/           # Color, Typography, Theme
│   └── navigation/      # AppViewModel, NavGraph
├── monetization/        # AdManager, BillingManager, MonetizationState (all expect/actual)
├── util/                # TimeUtil
└── App.kt               # NavHost with 5-screen navigation
```

## Game Engine

- **Structure**: Best-of-5 rounds, 10 questions/round, 5-second time limit per question
- **State Machine**: 8 states (Idle → MatchReady → RoundStarting → QuestionActive → QuestionResult → RoundComplete → MatchComplete)
- **Word Data**: `antonym_pairs.json` with 3000+ pairs and difficulty levels

### Bot AI (`BotBehavior`)

| Difficulty | Accuracy | Response Time | Bot Name |
|-----------|----------|---------------|----------|
| EASY | 50-70% | 3-4.5s | Rookie Bot |
| MEDIUM | 70-85% | 1.5-3s | Challenger Bot |
| HARD | 85-95% | 0.8-2s | Expert Bot |
| EXPERT | 95-100% | 0.5-1.5s | Master Bot |

### Scoring (`ScoringEngine`)

- Base: 100 points per correct answer
- Time bonus: up to 50 points (linear decay over 5s)
- Streak multipliers: 1.0x (0) → 1.5x (1-2) → 2.0x (3) → 2.5x (4) → 3.0x (5+)
- Max per question: 450 points

### Ranking System

7 tiers: Bronze (0) → Silver (1K) → Gold (3K) → Platinum (6K) → Diamond (10K) → Master (15K) → Grandmaster (25K)

## Monetization

- **Ads**: AdMob interstitials (3-min cooldown) and rewarded ads (hints, bonus points)
- **IAP**: "remove_ads" one-time purchase via Google Play Billing / StoreKit
- **Hints**: Max 2 per match, eliminates 2 wrong answers, requires rewarded ad
- iOS monetization uses stub classes (implementation pending)

## Key Files

- `domain/game/GameEngine.kt` - Main orchestration: rounds, questions, bot scheduling
- `domain/game/GameState.kt` - Sealed class hierarchy for 8 game states
- `domain/game/ScoringEngine.kt` - Points calculation with streaks
- `domain/bot/BotBehavior.kt` - AI decision logic per difficulty
- `domain/ranking/RankingSystem.kt` - Match results and rank calculation
- `data/repository/WordRepository.kt` - Loads and caches antonym pairs
- `data/source/SettingsStorage.kt` - Platform-specific persistence

## Development Notes

- `composeResources/files/antonym_pairs.json` contains 3000+ word pairs
- Docker support for consistent dev environment (Java 17 + Node.js 20)
- Versions managed in `gradle/libs.versions.toml`
