package com.markduenas.antonymizer.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Screen {
    @Serializable
    data object Home : Screen()

    @Serializable
    data class Game(val difficulty: String) : Screen()

    @Serializable
    data class Results(
        val playerScore: Int,
        val botScore: Int,
        val playerRoundsWon: Int,
        val botRoundsWon: Int,
        val winner: String,
        val longestStreak: Int,
        val fastestAnswer: Long,
        val accuracy: Float,
        val difficulty: String
    ) : Screen()

    @Serializable
    data object Leaderboard : Screen()

    @Serializable
    data object Settings : Screen()
}
