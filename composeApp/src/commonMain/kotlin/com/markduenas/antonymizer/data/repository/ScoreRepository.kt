package com.markduenas.antonymizer.data.repository

import com.markduenas.antonymizer.data.model.BotDifficulty
import com.markduenas.antonymizer.data.model.LeaderboardEntry
import com.markduenas.antonymizer.data.model.PlayerStats
import com.markduenas.antonymizer.data.source.SettingsStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface ScoreRepository {
    fun getPlayerStats(): Flow<PlayerStats>
    suspend fun updatePlayerStats(stats: PlayerStats)
    fun getLeaderboard(): Flow<List<LeaderboardEntry>>
    suspend fun addLeaderboardEntry(entry: LeaderboardEntry)
    suspend fun clearLeaderboard()
}

class ScoreRepositoryImpl(
    private val settingsStorage: SettingsStorage
) : ScoreRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun getPlayerStats(): Flow<PlayerStats> {
        return settingsStorage.getPlayerStats().map { statsJson ->
            statsJson?.let {
                try {
                    json.decodeFromString<PlayerStats>(it)
                } catch (e: Exception) {
                    PlayerStats()
                }
            } ?: PlayerStats()
        }
    }

    override suspend fun updatePlayerStats(stats: PlayerStats) {
        val statsJson = json.encodeToString(stats)
        settingsStorage.setPlayerStats(statsJson)
    }

    override fun getLeaderboard(): Flow<List<LeaderboardEntry>> {
        return settingsStorage.getLeaderboard().map { leaderboardJson ->
            leaderboardJson?.let {
                try {
                    json.decodeFromString<List<LeaderboardEntry>>(it)
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()
        }
    }

    override suspend fun addLeaderboardEntry(entry: LeaderboardEntry) {
        val currentLeaderboard = getLeaderboard().first().toMutableList()
        currentLeaderboard.add(entry)

        // Sort by score descending and keep top 100
        val sortedLeaderboard = currentLeaderboard
            .sortedByDescending { it.score }
            .take(100)

        val leaderboardJson = json.encodeToString(sortedLeaderboard)
        settingsStorage.setLeaderboard(leaderboardJson)
    }

    override suspend fun clearLeaderboard() {
        settingsStorage.setLeaderboard("[]")
    }
}
