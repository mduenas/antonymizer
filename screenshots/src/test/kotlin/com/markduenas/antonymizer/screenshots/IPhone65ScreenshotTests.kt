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
import com.github.takahirom.roborazzi.captureRoboImage
import com.markduenas.antonymizer.data.model.BotDifficulty
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

/**
 * Screenshot tests for iPhone 6.5" display (1284 x 2778 pixels)
 * Required for: iPhone 14 Plus, iPhone 13 Pro Max, iPhone 12 Pro Max, iPhone 11 Pro Max, iPhone XS Max
 *
 * Calculation: Using xxhdpi (480 dpi) -> 1284/3 = 428dp width, 2778/3 = 926dp height
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w428dp-h926dp-xxhdpi")
class IPhone65ScreenshotTests {

    @get:Rule
    val composeRule = createComposeRule()

    // ==================== HOME SCREEN ====================

    @Test
    fun homeScreen_iPhone65() {
        composeRule.setContent {
            ScreenshotWrapper {
                HomeScreen(
                    playerStats = ScreenshotData.experiencedPlayerStats,
                    playerName = "WordMaster",
                    onPlayClick = {},
                    onLeaderboardClick = {},
                    onSettingsClick = {}
                )
            }
        }
        composeRule.onRoot().captureRoboImage("screenshots/ios/iphone65/homeScreen.png")
    }

    @Test
    fun homeScreen_dark_iPhone65() {
        composeRule.setContent {
            ScreenshotWrapper(darkTheme = true) {
                HomeScreen(
                    playerStats = ScreenshotData.experiencedPlayerStats,
                    playerName = "WordMaster",
                    onPlayClick = {},
                    onLeaderboardClick = {},
                    onSettingsClick = {}
                )
            }
        }
        composeRule.onRoot().captureRoboImage("screenshots/ios/iphone65/homeScreen_dark.png")
    }

    // ==================== GAME SCREEN ====================

    @Test
    fun gameScreen_questionActive_iPhone65() {
        composeRule.setContent {
            ScreenshotWrapper {
                GameScreen(
                    gameState = ScreenshotData.questionActiveState,
                    onAnswerSelected = {},
                    onStartMatch = {},
                    onExitGame = {},
                    onMatchComplete = {}
                )
            }
        }
        composeRule.onRoot().captureRoboImage("screenshots/ios/iphone65/gameScreen_questionActive.png")
    }

    @Test
    fun gameScreen_questionActive_dark_iPhone65() {
        composeRule.setContent {
            ScreenshotWrapper(darkTheme = true) {
                GameScreen(
                    gameState = ScreenshotData.questionActiveState,
                    onAnswerSelected = {},
                    onStartMatch = {},
                    onExitGame = {},
                    onMatchComplete = {}
                )
            }
        }
        composeRule.onRoot().captureRoboImage("screenshots/ios/iphone65/gameScreen_questionActive_dark.png")
    }

    @Test
    fun gameScreen_highStreak_iPhone65() {
        composeRule.setContent {
            ScreenshotWrapper {
                GameScreen(
                    gameState = ScreenshotData.questionActiveHighStreakState,
                    onAnswerSelected = {},
                    onStartMatch = {},
                    onExitGame = {},
                    onMatchComplete = {}
                )
            }
        }
        composeRule.onRoot().captureRoboImage("screenshots/ios/iphone65/gameScreen_highStreak.png")
    }

    // ==================== RESULTS SCREEN ====================

    @Test
    fun resultsScreen_victory_iPhone65() {
        composeRule.setContent {
            ScreenshotWrapper {
                ResultsScreen(
                    result = ScreenshotData.victoryResult,
                    difficulty = BotDifficulty.HARD,
                    onPlayAgain = {},
                    onGoHome = {}
                )
            }
        }
        composeRule.onRoot().captureRoboImage("screenshots/ios/iphone65/resultsScreen_victory.png")
    }

    @Test
    fun resultsScreen_victory_dark_iPhone65() {
        composeRule.setContent {
            ScreenshotWrapper(darkTheme = true) {
                ResultsScreen(
                    result = ScreenshotData.victoryResult,
                    difficulty = BotDifficulty.HARD,
                    onPlayAgain = {},
                    onGoHome = {}
                )
            }
        }
        composeRule.onRoot().captureRoboImage("screenshots/ios/iphone65/resultsScreen_victory_dark.png")
    }

    // ==================== LEADERBOARD SCREEN ====================

    @Test
    fun leaderboardScreen_iPhone65() {
        composeRule.setContent {
            ScreenshotWrapper {
                LeaderboardScreen(
                    entries = ScreenshotData.leaderboardEntries,
                    onBackClick = {}
                )
            }
        }
        composeRule.onRoot().captureRoboImage("screenshots/ios/iphone65/leaderboardScreen.png")
    }

    @Test
    fun leaderboardScreen_dark_iPhone65() {
        composeRule.setContent {
            ScreenshotWrapper(darkTheme = true) {
                LeaderboardScreen(
                    entries = ScreenshotData.leaderboardEntries,
                    onBackClick = {}
                )
            }
        }
        composeRule.onRoot().captureRoboImage("screenshots/ios/iphone65/leaderboardScreen_dark.png")
    }

    // ==================== SETTINGS SCREEN ====================

    @Test
    fun settingsScreen_iPhone65() {
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
        composeRule.onRoot().captureRoboImage("screenshots/ios/iphone65/settingsScreen.png")
    }

    @Test
    fun settingsScreen_dark_iPhone65() {
        composeRule.setContent {
            ScreenshotWrapper(darkTheme = true) {
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
        composeRule.onRoot().captureRoboImage("screenshots/ios/iphone65/settingsScreen_dark.png")
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
