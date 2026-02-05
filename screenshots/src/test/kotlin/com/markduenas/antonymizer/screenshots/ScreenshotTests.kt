package com.markduenas.antonymizer.screenshots

import android.graphics.Color
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.markduenas.antonymizer.data.model.BotDifficulty
import com.markduenas.antonymizer.data.model.LeaderboardEntry
import com.markduenas.antonymizer.data.model.PlayerStats
import com.markduenas.antonymizer.data.model.Rank
import com.markduenas.antonymizer.domain.game.GameState
import com.markduenas.antonymizer.domain.game.MatchResult
import com.markduenas.antonymizer.domain.game.PlayerAnswer
import com.markduenas.antonymizer.domain.game.Question
import com.markduenas.antonymizer.domain.game.RoundResult
import com.markduenas.antonymizer.domain.game.Winner
import com.markduenas.antonymizer.ui.screens.game.GameScreen
import com.markduenas.antonymizer.ui.screens.home.HomeScreen
import com.markduenas.antonymizer.ui.screens.leaderboard.LeaderboardScreen
import com.markduenas.antonymizer.ui.screens.results.ResultsScreen
import com.markduenas.antonymizer.ui.screens.settings.SettingsScreen
import com.markduenas.antonymizer.ui.theme.AntonymArenaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel6)
class ScreenshotTests {

    @get:Rule
    val composeRule = createComposeRule()

    // ==================== HOME SCREEN ====================

    @Test
    fun homeScreen_newPlayer() {
        composeRule.setContent {
            ScreenshotWrapper {
                HomeScreen(
                    playerStats = PlayerStats(),
                    playerName = "Player",
                    onPlayClick = {},
                    onLeaderboardClick = {},
                    onSettingsClick = {}
                )
            }
        }
        composeRule.onRoot().captureRoboImage("screenshots/homeScreen_newPlayer.png")
    }

    @Test
    fun homeScreen_experiencedPlayer() {
        composeRule.setContent {
            ScreenshotWrapper {
                HomeScreen(
                    playerStats = PlayerStats(
                        totalPoints = 4250,
                        totalGamesPlayed = 47,
                        totalWins = 32,
                        totalLosses = 15,
                        currentWinStreak = 5,
                        longestWinStreak = 8,
                        totalCorrectAnswers = 1850,
                        totalAnswers = 2350,
                        fastestAnswer = 892L,
                        longestStreak = 12
                    ),
                    playerName = "WordMaster",
                    onPlayClick = {},
                    onLeaderboardClick = {},
                    onSettingsClick = {}
                )
            }
        }
        composeRule.onRoot().captureRoboImage("screenshots/homeScreen_experiencedPlayer.png")
    }

    @Test
    fun homeScreen_grandmaster() {
        composeRule.setContent {
            ScreenshotWrapper {
                HomeScreen(
                    playerStats = PlayerStats(
                        totalPoints = 28500,
                        totalGamesPlayed = 156,
                        totalWins = 142,
                        totalLosses = 14,
                        currentWinStreak = 23,
                        longestWinStreak = 31,
                        totalCorrectAnswers = 6200,
                        totalAnswers = 7800,
                        fastestAnswer = 456L,
                        longestStreak = 18
                    ),
                    playerName = "LexiconLord",
                    onPlayClick = {},
                    onLeaderboardClick = {},
                    onSettingsClick = {}
                )
            }
        }
        composeRule.onRoot().captureRoboImage("screenshots/homeScreen_grandmaster.png")
    }

    // ==================== GAME SCREEN ====================

    @Test
    fun gameScreen_matchReady() {
        composeRule.setContent {
            ScreenshotWrapper {
                GameScreen(
                    gameState = GameState.MatchReady(
                        difficulty = BotDifficulty.HARD,
                        playerName = "WordMaster"
                    ),
                    onAnswerSelected = {},
                    onStartMatch = {},
                    onExitGame = {},
                    onMatchComplete = {}
                )
            }
        }
        composeRule.onRoot().captureRoboImage("screenshots/gameScreen_matchReady.png")
    }

    @Test
    fun gameScreen_roundStarting() {
        composeRule.setContent {
            ScreenshotWrapper {
                GameScreen(
                    gameState = GameState.RoundStarting(
                        roundNumber = 1,
                        totalRounds = 5,
                        countdown = 3
                    ),
                    onAnswerSelected = {},
                    onStartMatch = {},
                    onExitGame = {},
                    onMatchComplete = {}
                )
            }
        }
        composeRule.onRoot().captureRoboImage("screenshots/gameScreen_roundStarting.png")
    }

    @Test
    fun gameScreen_questionActive() {
        composeRule.setContent {
            ScreenshotWrapper {
                GameScreen(
                    gameState = GameState.QuestionActive(
                        roundNumber = 2,
                        questionNumber = 7,
                        totalQuestions = 10,
                        question = Question(
                            word = "HAPPY",
                            correctAnswer = "sad",
                            options = listOf("sad", "joyful", "content", "pleased"),
                            timeLimit = 5000L
                        ),
                        remainingTimeMs = 3500L,
                        playerScore = 420,
                        botScore = 380,
                        playerStreak = 4,
                        botStreak = 1,
                        playerAnswered = false,
                        botAnswered = false
                    ),
                    onAnswerSelected = {},
                    onStartMatch = {},
                    onExitGame = {},
                    onMatchComplete = {}
                )
            }
        }
        composeRule.onRoot().captureRoboImage("screenshots/gameScreen_questionActive.png")
    }

    @Test
    fun gameScreen_questionActive_highStreak() {
        composeRule.setContent {
            ScreenshotWrapper {
                GameScreen(
                    gameState = GameState.QuestionActive(
                        roundNumber = 3,
                        questionNumber = 5,
                        totalQuestions = 10,
                        question = Question(
                            word = "ANCIENT",
                            correctAnswer = "modern",
                            options = listOf("old", "modern", "historic", "vintage"),
                            timeLimit = 5000L
                        ),
                        remainingTimeMs = 4200L,
                        playerScore = 650,
                        botScore = 490,
                        playerStreak = 7,
                        botStreak = 0,
                        playerAnswered = false,
                        botAnswered = false
                    ),
                    onAnswerSelected = {},
                    onStartMatch = {},
                    onExitGame = {},
                    onMatchComplete = {}
                )
            }
        }
        composeRule.onRoot().captureRoboImage("screenshots/gameScreen_questionActive_highStreak.png")
    }

    @Test
    fun gameScreen_questionResult() {
        composeRule.setContent {
            ScreenshotWrapper {
                GameScreen(
                    gameState = GameState.QuestionResult(
                        roundNumber = 2,
                        questionNumber = 7,
                        question = Question(
                            word = "HAPPY",
                            correctAnswer = "sad",
                            options = listOf("sad", "joyful", "content", "pleased"),
                            timeLimit = 5000L
                        ),
                        playerAnswer = PlayerAnswer(
                            answer = "sad",
                            isCorrect = true,
                            responseTimeMs = 1245L,
                            pointsEarned = 85
                        ),
                        botAnswer = PlayerAnswer(
                            answer = "joyful",
                            isCorrect = false,
                            responseTimeMs = 2100L,
                            pointsEarned = 0
                        ),
                        playerTotalScore = 505,
                        botTotalScore = 380
                    ),
                    onAnswerSelected = {},
                    onStartMatch = {},
                    onExitGame = {},
                    onMatchComplete = {}
                )
            }
        }
        composeRule.onRoot().captureRoboImage("screenshots/gameScreen_questionResult.png")
    }

    @Test
    fun gameScreen_roundComplete() {
        composeRule.setContent {
            ScreenshotWrapper {
                GameScreen(
                    gameState = GameState.RoundComplete(
                        roundResult = RoundResult(
                            roundNumber = 2,
                            playerScore = 720,
                            botScore = 580,
                            playerCorrect = 8,
                            botCorrect = 6,
                            winner = Winner.PLAYER
                        ),
                        playerTotalScore = 1420,
                        botTotalScore = 1180,
                        playerRoundsWon = 2,
                        botRoundsWon = 0
                    ),
                    onAnswerSelected = {},
                    onStartMatch = {},
                    onExitGame = {},
                    onMatchComplete = {}
                )
            }
        }
        composeRule.onRoot().captureRoboImage("screenshots/gameScreen_roundComplete.png")
    }

    // ==================== RESULTS SCREEN ====================

    @Test
    fun resultsScreen_victory() {
        composeRule.setContent {
            ScreenshotWrapper {
                ResultsScreen(
                    result = MatchResult(
                        playerTotalScore = 3250,
                        botTotalScore = 2780,
                        playerRoundsWon = 3,
                        botRoundsWon = 1,
                        winner = Winner.PLAYER,
                        roundResults = listOf(
                            RoundResult(1, 720, 580, 8, 6, Winner.PLAYER),
                            RoundResult(2, 650, 690, 7, 7, Winner.BOT),
                            RoundResult(3, 810, 720, 9, 8, Winner.PLAYER),
                            RoundResult(4, 1070, 790, 10, 8, Winner.PLAYER)
                        ),
                        longestStreak = 8,
                        fastestAnswer = 892L,
                        accuracy = 0.85f
                    ),
                    difficulty = BotDifficulty.HARD,
                    onPlayAgain = {},
                    onGoHome = {}
                )
            }
        }
        composeRule.onRoot().captureRoboImage("screenshots/resultsScreen_victory.png")
    }

    @Test
    fun resultsScreen_defeat() {
        composeRule.setContent {
            ScreenshotWrapper {
                ResultsScreen(
                    result = MatchResult(
                        playerTotalScore = 2450,
                        botTotalScore = 3120,
                        playerRoundsWon = 1,
                        botRoundsWon = 3,
                        winner = Winner.BOT,
                        roundResults = listOf(
                            RoundResult(1, 580, 720, 6, 8, Winner.BOT),
                            RoundResult(2, 710, 650, 7, 7, Winner.PLAYER),
                            RoundResult(3, 520, 810, 5, 9, Winner.BOT),
                            RoundResult(4, 640, 940, 6, 10, Winner.BOT)
                        ),
                        longestStreak = 4,
                        fastestAnswer = 1250L,
                        accuracy = 0.60f
                    ),
                    difficulty = BotDifficulty.EXPERT,
                    onPlayAgain = {},
                    onGoHome = {}
                )
            }
        }
        composeRule.onRoot().captureRoboImage("screenshots/resultsScreen_defeat.png")
    }

    @Test
    fun resultsScreen_closeMatch() {
        composeRule.setContent {
            ScreenshotWrapper {
                ResultsScreen(
                    result = MatchResult(
                        playerTotalScore = 4120,
                        botTotalScore = 4050,
                        playerRoundsWon = 3,
                        botRoundsWon = 2,
                        winner = Winner.PLAYER,
                        roundResults = listOf(
                            RoundResult(1, 780, 820, 8, 8, Winner.BOT),
                            RoundResult(2, 850, 790, 9, 8, Winner.PLAYER),
                            RoundResult(3, 810, 850, 8, 9, Winner.BOT),
                            RoundResult(4, 870, 810, 9, 8, Winner.PLAYER),
                            RoundResult(5, 810, 780, 8, 8, Winner.PLAYER)
                        ),
                        longestStreak = 6,
                        fastestAnswer = 756L,
                        accuracy = 0.84f
                    ),
                    difficulty = BotDifficulty.HARD,
                    onPlayAgain = {},
                    onGoHome = {}
                )
            }
        }
        composeRule.onRoot().captureRoboImage("screenshots/resultsScreen_closeMatch.png")
    }

    // ==================== LEADERBOARD SCREEN ====================

    @Test
    fun leaderboardScreen_withEntries() {
        composeRule.setContent {
            ScreenshotWrapper {
                LeaderboardScreen(
                    entries = listOf(
                        LeaderboardEntry(
                            id = "1",
                            playerName = "LexiconLord",
                            score = 4250,
                            rank = Rank.GOLD,
                            timestamp = System.currentTimeMillis(),
                            difficulty = BotDifficulty.EXPERT,
                            roundsWon = 3,
                            totalRounds = 4
                        ),
                        LeaderboardEntry(
                            id = "2",
                            playerName = "WordMaster",
                            score = 3890,
                            rank = Rank.GOLD,
                            timestamp = System.currentTimeMillis() - 3600000,
                            difficulty = BotDifficulty.HARD,
                            roundsWon = 3,
                            totalRounds = 5
                        ),
                        LeaderboardEntry(
                            id = "3",
                            playerName = "VocabViking",
                            score = 3540,
                            rank = Rank.GOLD,
                            timestamp = System.currentTimeMillis() - 7200000,
                            difficulty = BotDifficulty.HARD,
                            roundsWon = 3,
                            totalRounds = 4
                        ),
                        LeaderboardEntry(
                            id = "4",
                            playerName = "SynonymSage",
                            score = 2980,
                            rank = Rank.SILVER,
                            timestamp = System.currentTimeMillis() - 10800000,
                            difficulty = BotDifficulty.MEDIUM,
                            roundsWon = 3,
                            totalRounds = 5
                        ),
                        LeaderboardEntry(
                            id = "5",
                            playerName = "AntonymAce",
                            score = 2650,
                            rank = Rank.SILVER,
                            timestamp = System.currentTimeMillis() - 14400000,
                            difficulty = BotDifficulty.MEDIUM,
                            roundsWon = 3,
                            totalRounds = 4
                        )
                    ),
                    onBackClick = {}
                )
            }
        }
        composeRule.onRoot().captureRoboImage("screenshots/leaderboardScreen_withEntries.png")
    }

    @Test
    fun leaderboardScreen_empty() {
        composeRule.setContent {
            ScreenshotWrapper {
                LeaderboardScreen(
                    entries = emptyList(),
                    onBackClick = {}
                )
            }
        }
        composeRule.onRoot().captureRoboImage("screenshots/leaderboardScreen_empty.png")
    }

    // ==================== SETTINGS SCREEN ====================

    @Test
    fun settingsScreen() {
        composeRule.setContent {
            ScreenshotWrapper {
                SettingsScreen(
                    playerName = "WordMaster",
                    soundEnabled = true,
                    vibrationEnabled = true,
                    onPlayerNameChange = {},
                    onSoundEnabledChange = {},
                    onVibrationEnabledChange = {},
                    onClearData = {},
                    onBackClick = {}
                )
            }
        }
        composeRule.onRoot().captureRoboImage("screenshots/settingsScreen.png")
    }

    @Test
    fun settingsScreen_audioDisabled() {
        composeRule.setContent {
            ScreenshotWrapper {
                SettingsScreen(
                    playerName = "QuietPlayer",
                    soundEnabled = false,
                    vibrationEnabled = false,
                    onPlayerNameChange = {},
                    onSoundEnabledChange = {},
                    onVibrationEnabledChange = {},
                    onClearData = {},
                    onBackClick = {}
                )
            }
        }
        composeRule.onRoot().captureRoboImage("screenshots/settingsScreen_audioDisabled.png")
    }
}

@Composable
private fun ScreenshotWrapper(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    AntonymArenaTheme(darkTheme = darkTheme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            content()
        }
    }
}
