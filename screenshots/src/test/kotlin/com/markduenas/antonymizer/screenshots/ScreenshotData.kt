package com.markduenas.antonymizer.screenshots

import com.markduenas.antonymizer.data.model.BotDifficulty
import com.markduenas.antonymizer.data.model.LeaderboardEntry
import com.markduenas.antonymizer.data.model.PlayerStats
import com.markduenas.antonymizer.data.model.Rank
import com.markduenas.antonymizer.domain.game.GameState
import com.markduenas.antonymizer.domain.game.MatchResult
import com.markduenas.antonymizer.domain.game.PlayerAnswer
import com.markduenas.antonymizer.domain.game.Question
import com.markduenas.antonymizer.domain.game.RoundResult
import com.markduenas.antonymizer.domain.game.Winner

/**
 * Shared demo data for screenshot generation across all device configurations.
 */
object ScreenshotData {

    val experiencedPlayerStats = PlayerStats(
        totalPoints = 4250,
        totalGamesPlayed = 47,
        totalWins = 32,
        totalLosses = 15,
        currentWinStreak = 5,
        longestWinStreak = 8,
        totalCorrectAnswers = 1850,
        totalAnswers = 2350,
        fastestAnswer = 892L,
        longestStreak = 12
    )

    val grandmasterStats = PlayerStats(
        totalPoints = 28500,
        totalGamesPlayed = 156,
        totalWins = 142,
        totalLosses = 14,
        currentWinStreak = 23,
        longestWinStreak = 31,
        totalCorrectAnswers = 6200,
        totalAnswers = 7800,
        fastestAnswer = 456L,
        longestStreak = 18
    )

    val questionActiveState = GameState.QuestionActive(
        roundNumber = 2,
        questionNumber = 7,
        totalQuestions = 10,
        question = Question(
            word = "HAPPY",
            correctAnswer = "sad",
            options = listOf("sad", "joyful", "content", "pleased"),
            timeLimit = 5000L
        ),
        remainingTimeMs = 3500L,
        playerScore = 420,
        botScore = 380,
        playerStreak = 4,
        botStreak = 1,
        playerAnswered = false,
        botAnswered = false
    )

    val questionActiveHighStreakState = GameState.QuestionActive(
        roundNumber = 3,
        questionNumber = 5,
        totalQuestions = 10,
        question = Question(
            word = "ANCIENT",
            correctAnswer = "modern",
            options = listOf("old", "modern", "historic", "vintage"),
            timeLimit = 5000L
        ),
        remainingTimeMs = 4200L,
        playerScore = 650,
        botScore = 490,
        playerStreak = 7,
        botStreak = 0,
        playerAnswered = false,
        botAnswered = false
    )

    val victoryResult = MatchResult(
        playerTotalScore = 3250,
        botTotalScore = 2780,
        playerRoundsWon = 3,
        botRoundsWon = 1,
        winner = Winner.PLAYER,
        roundResults = listOf(
            RoundResult(1, 720, 580, 8, 6, Winner.PLAYER),
            RoundResult(2, 650, 690, 7, 7, Winner.BOT),
            RoundResult(3, 810, 720, 9, 8, Winner.PLAYER),
            RoundResult(4, 1070, 790, 10, 8, Winner.PLAYER)
        ),
        longestStreak = 8,
        fastestAnswer = 892L,
        accuracy = 0.85f
    )

    val defeatResult = MatchResult(
        playerTotalScore = 2450,
        botTotalScore = 3120,
        playerRoundsWon = 1,
        botRoundsWon = 3,
        winner = Winner.BOT,
        roundResults = listOf(
            RoundResult(1, 580, 720, 6, 8, Winner.BOT),
            RoundResult(2, 710, 650, 7, 7, Winner.PLAYER),
            RoundResult(3, 520, 810, 5, 9, Winner.BOT),
            RoundResult(4, 640, 940, 6, 10, Winner.BOT)
        ),
        longestStreak = 4,
        fastestAnswer = 1250L,
        accuracy = 0.60f
    )

    val leaderboardEntries = listOf(
        LeaderboardEntry(
            id = "1",
            playerName = "LexiconLord",
            score = 4250,
            rank = Rank.GOLD,
            timestamp = System.currentTimeMillis(),
            difficulty = BotDifficulty.EXPERT,
            roundsWon = 3,
            totalRounds = 4
        ),
        LeaderboardEntry(
            id = "2",
            playerName = "WordMaster",
            score = 3890,
            rank = Rank.GOLD,
            timestamp = System.currentTimeMillis() - 3600000,
            difficulty = BotDifficulty.HARD,
            roundsWon = 3,
            totalRounds = 5
        ),
        LeaderboardEntry(
            id = "3",
            playerName = "VocabViking",
            score = 3540,
            rank = Rank.GOLD,
            timestamp = System.currentTimeMillis() - 7200000,
            difficulty = BotDifficulty.HARD,
            roundsWon = 3,
            totalRounds = 4
        ),
        LeaderboardEntry(
            id = "4",
            playerName = "SynonymSage",
            score = 2980,
            rank = Rank.SILVER,
            timestamp = System.currentTimeMillis() - 10800000,
            difficulty = BotDifficulty.MEDIUM,
            roundsWon = 3,
            totalRounds = 5
        ),
        LeaderboardEntry(
            id = "5",
            playerName = "AntonymAce",
            score = 2650,
            rank = Rank.SILVER,
            timestamp = System.currentTimeMillis() - 14400000,
            difficulty = BotDifficulty.MEDIUM,
            roundsWon = 3,
            totalRounds = 4
        )
    )
}
