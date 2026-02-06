package com.markduenas.antonymizer.data.source

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath

expect fun createDataStore(): DataStore<Preferences>

internal const val DATA_STORE_FILE_NAME = "antonymizer_prefs.preferences_pb"

fun createDataStoreWithPath(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath().toPath() }
    )

object PreferencesKeys {
    val PLAYER_NAME = stringPreferencesKey("player_name")
    val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
    val MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
    val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
    val DIFFICULTY = stringPreferencesKey("difficulty")
    val PLAYER_STATS = stringPreferencesKey("player_stats")
    val LEADERBOARD = stringPreferencesKey("leaderboard")
    val GAMES_PLAYED = intPreferencesKey("games_played")

    // Monetization keys
    val ADS_REMOVED = booleanPreferencesKey("ads_removed")
    val HINT_TOKENS = intPreferencesKey("hint_tokens")
    val LAST_INTERSTITIAL_TIME = longPreferencesKey("last_interstitial_time")
    val TOTAL_ADS_WATCHED = intPreferencesKey("total_ads_watched")
}

class SettingsStorage(private val dataStore: DataStore<Preferences>) {

    val playerName: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.PLAYER_NAME] ?: "Player"
    }

    val soundEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SOUND_ENABLED] ?: true
    }

    val musicEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.MUSIC_ENABLED] ?: true
    }

    val vibrationEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.VIBRATION_ENABLED] ?: true
    }

    val difficulty: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DIFFICULTY] ?: "MEDIUM"
    }

    suspend fun setPlayerName(name: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PLAYER_NAME] = name
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SOUND_ENABLED] = enabled
        }
    }

    suspend fun setMusicEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.MUSIC_ENABLED] = enabled
        }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.VIBRATION_ENABLED] = enabled
        }
    }

    suspend fun setDifficulty(difficulty: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DIFFICULTY] = difficulty
        }
    }

    fun getPlayerStats(): Flow<String?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.PLAYER_STATS]
    }

    suspend fun setPlayerStats(statsJson: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PLAYER_STATS] = statsJson
        }
    }

    fun getLeaderboard(): Flow<String?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LEADERBOARD]
    }

    suspend fun setLeaderboard(leaderboardJson: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LEADERBOARD] = leaderboardJson
        }
    }

    // Monetization preferences
    val adsRemoved: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ADS_REMOVED] ?: false
    }

    val hintTokens: Flow<Int> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.HINT_TOKENS] ?: 0
    }

    val lastInterstitialTime: Flow<Long> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LAST_INTERSTITIAL_TIME] ?: 0L
    }

    val totalAdsWatched: Flow<Int> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.TOTAL_ADS_WATCHED] ?: 0
    }

    suspend fun setAdsRemoved(removed: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ADS_REMOVED] = removed
        }
    }

    suspend fun setHintTokens(tokens: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.HINT_TOKENS] = tokens
        }
    }

    suspend fun setLastInterstitialTime(time: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_INTERSTITIAL_TIME] = time
        }
    }

    suspend fun incrementAdsWatched() {
        dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.TOTAL_ADS_WATCHED] ?: 0
            preferences[PreferencesKeys.TOTAL_ADS_WATCHED] = current + 1
        }
    }
}
