package com.markduenas.antonymizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.markduenas.antonymizer.data.model.BotDifficulty
import com.markduenas.antonymizer.data.source.SettingsStorage
import com.markduenas.antonymizer.domain.game.GameState
import com.markduenas.antonymizer.domain.game.MatchResult
import com.markduenas.antonymizer.domain.game.RoundResult
import com.markduenas.antonymizer.domain.game.Winner
import com.markduenas.antonymizer.monetization.MonetizationState
import com.markduenas.antonymizer.ui.navigation.AppViewModel
import com.markduenas.antonymizer.ui.navigation.Screen
import com.markduenas.antonymizer.ui.screens.game.GameScreen
import com.markduenas.antonymizer.ui.screens.home.HomeScreen
import com.markduenas.antonymizer.ui.screens.leaderboard.LeaderboardScreen
import com.markduenas.antonymizer.ui.screens.results.ResultsScreen
import com.markduenas.antonymizer.ui.screens.settings.SettingsScreen
import com.markduenas.antonymizer.ui.theme.AntonymArenaTheme

@Composable
fun App(settingsStorage: SettingsStorage, monetizationState: MonetizationState) {
    AntonymArenaTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            val viewModel = remember { AppViewModel(settingsStorage) }
            val navController = rememberNavController()

            val playerStats by viewModel.playerStats.collectAsState()
            val playerName by viewModel.playerName.collectAsState()
            val soundEnabled by viewModel.soundEnabled.collectAsState()
            val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
            val leaderboard by viewModel.leaderboard.collectAsState()
            val gameState by viewModel.gameState.collectAsState()

            // Monetization state
            val adsRemoved by monetizationState.adsRemoved.collectAsState()
            val rewardedReady by monetizationState.rewardedReady.collectAsState()
            val removeAdsPrice by monetizationState.removeAdsPrice.collectAsState()
            val purchaseState by monetizationState.purchaseState.collectAsState()

            NavHost(
                navController = navController,
                startDestination = Screen.Home
            ) {
                composable<Screen.Home> {
                    HomeScreen(
                        playerStats = playerStats,
                        playerName = playerName,
                        onPlayClick = { difficulty ->
                            viewModel.startGame(difficulty)
                            navController.navigate(Screen.Game(difficulty.name))
                        },
                        onLeaderboardClick = {
                            navController.navigate(Screen.Leaderboard)
                        },
                        onSettingsClick = {
                            navController.navigate(Screen.Settings)
                        }
                    )
                }

                composable<Screen.Game> { backStackEntry ->
                    val route = backStackEntry.toRoute<Screen.Game>()
                    val difficulty = BotDifficulty.valueOf(route.difficulty)

                    GameScreen(
                        gameState = gameState,
                        rewardedAdReady = rewardedReady,
                        onAnswerSelected = { answer ->
                            viewModel.submitAnswer(answer)
                        },
                        onStartMatch = {
                            viewModel.beginMatch()
                        },
                        onExitGame = {
                            viewModel.resetGame()
                            navController.popBackStack(Screen.Home, inclusive = false)
                        },
                        onHintRequested = { onRewarded, onDismissed ->
                            monetizationState.adManager.showRewardedAd(onRewarded, onDismissed)
                        },
                        onMatchComplete = { result ->
                            viewModel.onMatchComplete(result)
                            viewModel.resetGame()

                            // Show interstitial ad before navigating to results
                            monetizationState.adManager.showInterstitialIfEligible {
                                navController.navigate(
                                    Screen.Results(
                                        playerScore = result.playerTotalScore,
                                        botScore = result.botTotalScore,
                                        playerRoundsWon = result.playerRoundsWon,
                                        botRoundsWon = result.botRoundsWon,
                                        winner = result.winner.name,
                                        longestStreak = result.longestStreak,
                                        fastestAnswer = result.fastestAnswer,
                                        accuracy = result.accuracy,
                                        difficulty = difficulty.name
                                    )
                                ) {
                                    popUpTo(Screen.Home) { inclusive = false }
                                }
                            }
                        }
                    )
                }

                composable<Screen.Results> { backStackEntry ->
                    val route = backStackEntry.toRoute<Screen.Results>()
                    val difficulty = BotDifficulty.valueOf(route.difficulty)
                    val winner = Winner.valueOf(route.winner)

                    val result = MatchResult(
                        playerTotalScore = route.playerScore,
                        botTotalScore = route.botScore,
                        playerRoundsWon = route.playerRoundsWon,
                        botRoundsWon = route.botRoundsWon,
                        winner = winner,
                        roundResults = emptyList(),
                        longestStreak = route.longestStreak,
                        fastestAnswer = route.fastestAnswer,
                        accuracy = route.accuracy
                    )

                    ResultsScreen(
                        result = result,
                        difficulty = difficulty,
                        rewardedAdReady = rewardedReady,
                        onPlayAgain = {
                            viewModel.startGame(difficulty)
                            navController.navigate(Screen.Game(difficulty.name)) {
                                popUpTo(Screen.Home) { inclusive = false }
                            }
                        },
                        onGoHome = {
                            navController.popBackStack(Screen.Home, inclusive = false)
                        },
                        onContinueRequested = { onRewarded, onDismissed ->
                            monetizationState.adManager.showRewardedAd(onRewarded, onDismissed)
                        },
                        onBonusPointsRequested = { onRewarded, onDismissed ->
                            monetizationState.adManager.showRewardedAd(onRewarded, onDismissed)
                        }
                    )
                }

                composable<Screen.Leaderboard> {
                    LeaderboardScreen(
                        entries = leaderboard,
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }

                composable<Screen.Settings> {
                    SettingsScreen(
                        playerName = playerName,
                        soundEnabled = soundEnabled,
                        vibrationEnabled = vibrationEnabled,
                        adsRemoved = adsRemoved,
                        removeAdsPrice = removeAdsPrice,
                        purchaseState = purchaseState,
                        onPlayerNameChange = { name ->
                            viewModel.setPlayerName(name)
                        },
                        onSoundEnabledChange = { enabled ->
                            viewModel.setSoundEnabled(enabled)
                        },
                        onVibrationEnabledChange = { enabled ->
                            viewModel.setVibrationEnabled(enabled)
                        },
                        onClearData = {
                            viewModel.clearAllData()
                        },
                        onRemoveAdsPurchase = {
                            monetizationState.billingManager.purchaseRemoveAds()
                        },
                        onRestorePurchases = {
                            monetizationState.billingManager.restorePurchases()
                        },
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}
