package com.markduenas.antonymizer.domain.ranking

import com.markduenas.antonymizer.data.model.BotDifficulty
import com.markduenas.antonymizer.data.model.LeaderboardEntry
import com.markduenas.antonymizer.data.model.PlayerStats
import com.markduenas.antonymizer.data.model.Rank
import com.markduenas.antonymizer.data.repository.ScoreRepository
import com.markduenas.antonymizer.domain.game.MatchResult
import com.markduenas.antonymizer.domain.game.Winner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class RankingSystem(
    private val scoreRepository: ScoreRepository
) {
    fun getPlayerStats(): Flow<PlayerStats> {
        return scoreRepository.getPlayerStats()
    }

    suspend fun recordMatchResult(
        result: MatchResult,
        difficulty: BotDifficulty,
        playerName: String
    ) {
        val currentStats = scoreRepository.getPlayerStats().first()

        val won = result.winner == Winner.PLAYER

        val updatedStats = currentStats.withGameResult(
            won = won,
            pointsEarned = result.playerTotalScore,
            correctAnswers = result.roundResults.sumOf { it.playerCorrect },
            totalRoundAnswers = result.roundResults.sumOf { it.playerCorrect + (10 - it.playerCorrect) },
            fastestAnswerMs = result.fastestAnswer,
            longestStreakInGame = result.longestStreak
        )

        scoreRepository.updatePlayerStats(updatedStats)

        // Add to leaderboard if player won or scored high
        if (won || result.playerTotalScore > 0) {
            val entry = LeaderboardEntry.create(
                playerName = playerName,
                score = result.playerTotalScore,
                difficulty = difficulty,
                roundsWon = result.playerRoundsWon
            )
            scoreRepository.addLeaderboardEntry(entry)
        }
    }

    fun calculateRankProgress(stats: PlayerStats): RankProgress {
        val currentRank = stats.rank
        val pointsToNext = Rank.pointsToNextRank(stats.totalPoints)

        val currentRankMin = currentRank.minPoints
        val nextRankMin = Rank.entries.getOrNull(currentRank.ordinal + 1)?.minPoints

        val progress = if (nextRankMin != null) {
            val pointsInCurrentRank = stats.totalPoints - currentRankMin
            val pointsNeededForNext = nextRankMin - currentRankMin
            pointsInCurrentRank.toFloat() / pointsNeededForNext
        } else {
            1f // Max rank
        }

        return RankProgress(
            currentRank = currentRank,
            nextRank = Rank.entries.getOrNull(currentRank.ordinal + 1),
            progressPercent = progress,
            pointsToNextRank = pointsToNext
        )
    }
}

data class RankProgress(
    val currentRank: Rank,
    val nextRank: Rank?,
    val progressPercent: Float,
    val pointsToNextRank: Int?
)
