package com.markduenas.antonymizer.domain.game

import kotlin.math.max

class ScoringEngine {

    companion object {
        const val BASE_POINTS = 100
        const val MAX_TIME_BONUS = 50
        const val TIME_LIMIT_MS = 5000L

        val STREAK_MULTIPLIERS = mapOf(
            0 to 1.0f,
            1 to 1.0f,
            2 to 1.5f,
            3 to 2.0f,
            4 to 2.5f,
            5 to 3.0f
        )
    }

    fun calculatePoints(
        isCorrect: Boolean,
        responseTimeMs: Long,
        currentStreak: Int,
        timeLimit: Long = TIME_LIMIT_MS
    ): Int {
        if (!isCorrect) return 0

        val basePoints = BASE_POINTS

        // Time bonus: faster responses get more points
        // Full bonus at 0ms, 0 bonus at timeLimit
        val timeRatio = max(0.0, 1.0 - (responseTimeMs.toDouble() / timeLimit))
        val timeBonus = (MAX_TIME_BONUS * timeRatio).toInt()

        // Streak multiplier
        val streakMultiplier = getStreakMultiplier(currentStreak)

        val totalPoints = ((basePoints + timeBonus) * streakMultiplier).toInt()
        return totalPoints
    }

    fun getStreakMultiplier(streak: Int): Float {
        return when {
            streak >= 5 -> STREAK_MULTIPLIERS[5]!!
            streak > 0 -> STREAK_MULTIPLIERS[streak] ?: 1.0f
            else -> 1.0f
        }
    }

    fun calculateAccuracy(correctAnswers: Int, totalAnswers: Int): Float {
        if (totalAnswers == 0) return 0f
        return correctAnswers.toFloat() / totalAnswers
    }

    fun determineRoundWinner(playerScore: Int, botScore: Int): Winner {
        return when {
            playerScore > botScore -> Winner.PLAYER
            botScore > playerScore -> Winner.BOT
            else -> Winner.TIE
        }
    }

    fun determineMatchWinner(playerRoundsWon: Int, botRoundsWon: Int): Winner {
        return when {
            playerRoundsWon > botRoundsWon -> Winner.PLAYER
            botRoundsWon > playerRoundsWon -> Winner.BOT
            else -> Winner.TIE
        }
    }
}
