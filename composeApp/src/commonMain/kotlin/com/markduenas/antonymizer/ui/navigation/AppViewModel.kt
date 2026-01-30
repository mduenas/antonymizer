package com.markduenas.antonymizer.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markduenas.antonymizer.data.model.BotDifficulty
import com.markduenas.antonymizer.data.model.LeaderboardEntry
import com.markduenas.antonymizer.data.model.PlayerStats
import com.markduenas.antonymizer.data.repository.ScoreRepository
import com.markduenas.antonymizer.data.repository.ScoreRepositoryImpl
import com.markduenas.antonymizer.data.repository.WordRepository
import com.markduenas.antonymizer.data.repository.WordRepositoryImpl
import com.markduenas.antonymizer.data.source.SettingsStorage
import com.markduenas.antonymizer.domain.game.GameEngine
import com.markduenas.antonymizer.domain.game.GameState
import com.markduenas.antonymizer.domain.game.MatchResult
import com.markduenas.antonymizer.domain.ranking.LeaderboardManager
import com.markduenas.antonymizer.domain.ranking.RankingSystem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(
    private val settingsStorage: SettingsStorage,
    private val wordRepository: WordRepository = WordRepositoryImpl(),
    private val scoreRepository: ScoreRepository = ScoreRepositoryImpl(settingsStorage)
) : ViewModel() {

    private val rankingSystem = RankingSystem(scoreRepository)
    private val leaderboardManager = LeaderboardManager(scoreRepository)

    private var gameEngine: GameEngine? = null

    private val _gameState = MutableStateFlow<GameState>(GameState.Idle)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    val playerStats: StateFlow<PlayerStats> = scoreRepository.getPlayerStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerStats())

    val playerName: StateFlow<String> = settingsStorage.playerName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Player")

    val soundEnabled: StateFlow<Boolean> = settingsStorage.soundEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val vibrationEnabled: StateFlow<Boolean> = settingsStorage.vibrationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val leaderboard: StateFlow<List<LeaderboardEntry>> = scoreRepository.getLeaderboard()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var currentDifficulty: BotDifficulty = BotDifficulty.MEDIUM

    fun startGame(difficulty: BotDifficulty) {
        currentDifficulty = difficulty
        gameEngine = GameEngine(wordRepository, scope = viewModelScope)

        viewModelScope.launch {
            gameEngine?.gameState?.collect { state ->
                _gameState.value = state
            }
        }

        viewModelScope.launch {
            val name = playerName.first()
            gameEngine?.startMatch(difficulty, name)
        }
    }

    fun beginMatch() {
        gameEngine?.beginMatch()
    }

    fun submitAnswer(answer: String) {
        gameEngine?.submitAnswer(answer)
    }

    fun onMatchComplete(result: MatchResult) {
        viewModelScope.launch {
            val name = playerName.first()
            rankingSystem.recordMatchResult(result, currentDifficulty, name)
        }
    }

    fun resetGame() {
        gameEngine?.resetGame()
        gameEngine = null
        _gameState.value = GameState.Idle
    }

    fun setPlayerName(name: String) {
        viewModelScope.launch {
            settingsStorage.setPlayerName(name)
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStorage.setSoundEnabled(enabled)
        }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStorage.setVibrationEnabled(enabled)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            scoreRepository.updatePlayerStats(PlayerStats())
            leaderboardManager.clearLeaderboard()
            settingsStorage.setPlayerName("Player")
            settingsStorage.setSoundEnabled(true)
            settingsStorage.setVibrationEnabled(true)
        }
    }
}
