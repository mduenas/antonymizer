package com.markduenas.antonymizer.data.model

import com.markduenas.antonymizer.util.currentTimeMillis
import kotlinx.serialization.Serializable

@Serializable
data class LeaderboardEntry(
    val id: String,
    val playerName: String,
    val score: Int,
    val rank: Rank,
    val timestamp: Long,
    val difficulty: BotDifficulty,
    val roundsWon: Int,
    val totalRounds: Int
) {
    companion object {
        fun create(
            playerName: String,
            score: Int,
            difficulty: BotDifficulty,
            roundsWon: Int,
            totalRounds: Int = 5
        ): LeaderboardEntry {
            return LeaderboardEntry(
                id = generateId(),
                playerName = playerName,
                score = score,
                rank = Rank.fromPoints(score),
                timestamp = currentTimeMillis(),
                difficulty = difficulty,
                roundsWon = roundsWon,
                totalRounds = totalRounds
            )
        }

        private fun generateId(): String {
            return currentTimeMillis().toString() + "_" + (0..9999).random()
        }
    }
}

@Serializable
enum class BotDifficulty(
    val displayName: String,
    val accuracyRange: ClosedFloatingPointRange<Float>,
    val responseTimeRange: LongRange
) {
    EASY("Easy", 0.5f..0.7f, 3000L..4500L),
    MEDIUM("Medium", 0.7f..0.85f, 1500L..3000L),
    HARD("Hard", 0.85f..0.95f, 800L..2000L),
    EXPERT("Expert", 0.95f..1.0f, 500L..1500L)
}
