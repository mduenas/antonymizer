package com.markduenas.antonymizer.domain.ranking

import com.markduenas.antonymizer.data.model.BotDifficulty
import com.markduenas.antonymizer.data.model.LeaderboardEntry
import com.markduenas.antonymizer.data.repository.ScoreRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LeaderboardManager(
    private val scoreRepository: ScoreRepository
) {
    fun getLeaderboard(): Flow<List<LeaderboardEntry>> {
        return scoreRepository.getLeaderboard()
    }

    fun getLeaderboardByDifficulty(difficulty: BotDifficulty): Flow<List<LeaderboardEntry>> {
        return scoreRepository.getLeaderboard().map { entries ->
            entries.filter { it.difficulty == difficulty }
        }
    }

    fun getTopScores(limit: Int = 10): Flow<List<LeaderboardEntry>> {
        return scoreRepository.getLeaderboard().map { entries ->
            entries.take(limit)
        }
    }

    fun getPlayerPosition(playerName: String): Flow<Int?> {
        return scoreRepository.getLeaderboard().map { entries ->
            val index = entries.indexOfFirst { it.playerName == playerName }
            if (index >= 0) index + 1 else null
        }
    }

    fun getRecentGames(limit: Int = 10): Flow<List<LeaderboardEntry>> {
        return scoreRepository.getLeaderboard().map { entries ->
            entries.sortedByDescending { it.timestamp }.take(limit)
        }
    }

    suspend fun clearLeaderboard() {
        scoreRepository.clearLeaderboard()
    }
}
