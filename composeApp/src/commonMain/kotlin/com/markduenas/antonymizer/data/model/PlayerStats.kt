package com.markduenas.antonymizer.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PlayerStats(
    val totalPoints: Int = 0,
    val totalGamesPlayed: Int = 0,
    val totalWins: Int = 0,
    val totalLosses: Int = 0,
    val currentWinStreak: Int = 0,
    val longestWinStreak: Int = 0,
    val totalCorrectAnswers: Int = 0,
    val totalAnswers: Int = 0,
    val fastestAnswer: Long = Long.MAX_VALUE,
    val longestStreak: Int = 0
) {
    val rank: Rank
        get() = Rank.fromPoints(totalPoints)

    val winRate: Float
        get() = if (totalGamesPlayed > 0) {
            totalWins.toFloat() / totalGamesPlayed
        } else 0f

    val accuracy: Float
        get() = if (totalAnswers > 0) {
            totalCorrectAnswers.toFloat() / totalAnswers
        } else 0f

    fun withGameResult(
        won: Boolean,
        pointsEarned: Int,
        correctAnswers: Int,
        totalRoundAnswers: Int,
        fastestAnswerMs: Long,
        longestStreakInGame: Int
    ): PlayerStats {
        val newWinStreak = if (won) currentWinStreak + 1 else 0
        return copy(
            totalPoints = totalPoints + pointsEarned,
            totalGamesPlayed = totalGamesPlayed + 1,
            totalWins = if (won) totalWins + 1 else totalWins,
            totalLosses = if (!won) totalLosses + 1 else totalLosses,
            currentWinStreak = newWinStreak,
            longestWinStreak = maxOf(longestWinStreak, newWinStreak),
            totalCorrectAnswers = totalCorrectAnswers + correctAnswers,
            totalAnswers = totalAnswers + totalRoundAnswers,
            fastestAnswer = if (fastestAnswerMs < fastestAnswer) fastestAnswerMs else fastestAnswer,
            longestStreak = maxOf(longestStreak, longestStreakInGame)
        )
    }
}
