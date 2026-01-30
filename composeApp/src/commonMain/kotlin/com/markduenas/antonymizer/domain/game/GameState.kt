package com.markduenas.antonymizer.domain.game

import com.markduenas.antonymizer.data.model.AntonymPair
import com.markduenas.antonymizer.data.model.BotDifficulty

data class Question(
    val word: String,
    val correctAnswer: String,
    val options: List<String>,
    val timeLimit: Long = 5000L
)

data class PlayerAnswer(
    val answer: String?,
    val isCorrect: Boolean,
    val responseTimeMs: Long,
    val pointsEarned: Int
)

data class RoundResult(
    val roundNumber: Int,
    val playerScore: Int,
    val botScore: Int,
    val playerCorrect: Int,
    val botCorrect: Int,
    val winner: Winner
)

enum class Winner {
    PLAYER,
    BOT,
    TIE
}

data class MatchResult(
    val playerTotalScore: Int,
    val botTotalScore: Int,
    val playerRoundsWon: Int,
    val botRoundsWon: Int,
    val winner: Winner,
    val roundResults: List<RoundResult>,
    val longestStreak: Int,
    val fastestAnswer: Long,
    val accuracy: Float
)

sealed class GameState {
    data object Idle : GameState()

    data class Loading(
        val message: String = "Loading..."
    ) : GameState()

    data class MatchReady(
        val difficulty: BotDifficulty,
        val playerName: String
    ) : GameState()

    data class RoundStarting(
        val roundNumber: Int,
        val totalRounds: Int,
        val countdown: Int
    ) : GameState()

    data class QuestionActive(
        val roundNumber: Int,
        val questionNumber: Int,
        val totalQuestions: Int,
        val question: Question,
        val remainingTimeMs: Long,
        val playerScore: Int,
        val botScore: Int,
        val playerStreak: Int,
        val botStreak: Int,
        val playerAnswered: Boolean,
        val botAnswered: Boolean
    ) : GameState()

    data class QuestionResult(
        val roundNumber: Int,
        val questionNumber: Int,
        val question: Question,
        val playerAnswer: PlayerAnswer,
        val botAnswer: PlayerAnswer,
        val playerTotalScore: Int,
        val botTotalScore: Int
    ) : GameState()

    data class RoundComplete(
        val roundResult: RoundResult,
        val playerTotalScore: Int,
        val botTotalScore: Int,
        val playerRoundsWon: Int,
        val botRoundsWon: Int
    ) : GameState()

    data class MatchComplete(
        val result: MatchResult,
        val difficulty: BotDifficulty
    ) : GameState()

    data class Error(
        val message: String
    ) : GameState()
}

data class GameConfig(
    val totalRounds: Int = 5,
    val questionsPerRound: Int = 10,
    val timePerQuestion: Long = 5000L,
    val difficulty: BotDifficulty = BotDifficulty.MEDIUM
)
