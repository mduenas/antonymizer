package com.markduenas.antonymizer.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class Rank(
    val displayName: String,
    val minPoints: Int,
    val tier: Int
) {
    BRONZE("Bronze", 0, 1),
    SILVER("Silver", 1000, 2),
    GOLD("Gold", 3000, 3),
    PLATINUM("Platinum", 6000, 4),
    DIAMOND("Diamond", 10000, 5),
    MASTER("Master", 15000, 6),
    GRANDMASTER("Grandmaster", 25000, 7);

    companion object {
        fun fromPoints(points: Int): Rank {
            return entries.lastOrNull { points >= it.minPoints } ?: BRONZE
        }

        fun pointsToNextRank(currentPoints: Int): Int? {
            val currentRank = fromPoints(currentPoints)
            val nextRank = entries.getOrNull(currentRank.ordinal + 1)
            return nextRank?.let { it.minPoints - currentPoints }
        }
    }
}
