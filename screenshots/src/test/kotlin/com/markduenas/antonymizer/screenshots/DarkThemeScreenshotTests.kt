package com.markduenas.antonymizer.screenshots

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
class DarkThemeScreenshotTests {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeScreen_dark() {
        composeRule.setContent {
            DarkScreenshotWrapper {
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
        composeRule.onRoot().captureRoboImage("screenshots/homeScreen_dark.png")
    }

    @Test
    fun gameScreen_questionActive_dark() {
        composeRule.setContent {
            DarkScreenshotWrapper {
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
        composeRule.onRoot().captureRoboImage("screenshots/gameScreen_questionActive_dark.png")
    }

    @Test
    fun resultsScreen_victory_dark() {
        composeRule.setContent {
            DarkScreenshotWrapper {
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
        composeRule.onRoot().captureRoboImage("screenshots/resultsScreen_victory_dark.png")
    }

    @Test
    fun leaderboardScreen_dark() {
        composeRule.setContent {
            DarkScreenshotWrapper {
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
                        )
                    ),
                    onBackClick = {}
                )
            }
        }
        composeRule.onRoot().captureRoboImage("screenshots/leaderboardScreen_dark.png")
    }

    @Test
    fun settingsScreen_dark() {
        composeRule.setContent {
            DarkScreenshotWrapper {
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
        composeRule.onRoot().captureRoboImage("screenshots/settingsScreen_dark.png")
    }
}

@Composable
private fun DarkScreenshotWrapper(
    content: @Composable () -> Unit
) {
    AntonymArenaTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            content()
        }
    }
}
