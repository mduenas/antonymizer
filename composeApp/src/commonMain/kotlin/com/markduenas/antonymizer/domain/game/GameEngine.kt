package com.markduenas.antonymizer.domain.game

import com.markduenas.antonymizer.data.model.AntonymPair
import com.markduenas.antonymizer.data.model.BotDifficulty
import com.markduenas.antonymizer.data.repository.WordRepository
import com.markduenas.antonymizer.domain.bot.BotBehavior
import com.markduenas.antonymizer.domain.bot.BotDecision
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameEngine(
    private val wordRepository: WordRepository,
    private val scoringEngine: ScoringEngine = ScoringEngine(),
    private val scope: CoroutineScope
) {
    private val _gameState = MutableStateFlow<GameState>(GameState.Idle)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private var config = GameConfig()
    private var botBehavior: BotBehavior? = null
    private var playerName: String = "Player"

    // Match state
    private var currentRound = 0
    private var currentQuestion = 0
    private var questions: List<Question> = emptyList()

    // Round state
    private var playerRoundScore = 0
    private var botRoundScore = 0
    private var playerRoundCorrect = 0
    private var botRoundCorrect = 0

    // Match totals
    private var playerTotalScore = 0
    private var botTotalScore = 0
    private var playerRoundsWon = 0
    private var botRoundsWon = 0
    private var roundResults = mutableListOf<RoundResult>()

    // Streak tracking
    private var playerStreak = 0
    private var botStreak = 0
    private var longestPlayerStreak = 0
    private var fastestPlayerAnswer = Long.MAX_VALUE
    private var totalPlayerCorrect = 0
    private var totalQuestions = 0

    // Question state
    private var questionStartTime = 0L
    private var playerAnswer: PlayerAnswer? = null
    private var botDecision: BotDecision? = null
    private var timerJob: Job? = null
    private var botAnswerJob: Job? = null
    private var resultTransitionJob: Job? = null
    private var isProcessingQuestionEnd = false

    fun startMatch(difficulty: BotDifficulty, playerName: String) {
        this.playerName = playerName
        this.config = config.copy(difficulty = difficulty)
        this.botBehavior = BotBehavior(difficulty)

        resetMatchState()

        _gameState.value = GameState.MatchReady(
            difficulty = difficulty,
            playerName = playerName
        )
    }

    fun beginMatch() {
        scope.launch {
            startRound()
        }
    }

    private suspend fun startRound() {
        currentRound++
        currentQuestion = 0
        playerRoundScore = 0
        botRoundScore = 0
        playerRoundCorrect = 0
        botRoundCorrect = 0

        // Load questions for this round
        val pairs = wordRepository.getRandomPairs(config.questionsPerRound)
        questions = pairs.map { pair ->
            createQuestion(pair)
        }

        // Countdown
        for (i in 3 downTo 1) {
            _gameState.value = GameState.RoundStarting(
                roundNumber = currentRound,
                totalRounds = config.totalRounds,
                countdown = i
            )
            delay(1000)
        }

        startQuestion()
    }

    private suspend fun createQuestion(pair: AntonymPair): Question {
        val distractors = wordRepository.getDistractors(pair.antonym, 3)
        val options = (distractors + pair.antonym).shuffled()

        return Question(
            word = pair.word,
            correctAnswer = pair.antonym,
            options = options,
            timeLimit = config.timePerQuestion
        )
    }

    private fun startQuestion() {
        val question = questions[currentQuestion]
        questionStartTime = currentTimeMillis()
        playerAnswer = null
        botDecision = null

        _gameState.value = GameState.QuestionActive(
            roundNumber = currentRound,
            questionNumber = currentQuestion + 1,
            totalQuestions = config.questionsPerRound,
            question = question,
            remainingTimeMs = config.timePerQuestion,
            playerScore = playerRoundScore,
            botScore = botRoundScore,
            playerStreak = playerStreak,
            botStreak = botStreak,
            playerAnswered = false,
            botAnswered = false
        )

        // Schedule bot answer
        scheduleBotAnswer(question)

        // Start timer
        startTimer()
    }

    private fun scheduleBotAnswer(question: Question) {
        val decision = botBehavior?.makeDecision(question) ?: return

        botAnswerJob = scope.launch {
            delay(decision.responseTimeMs)
            if (botDecision == null) {
                botDecision = decision
                checkBothAnswered()
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            var remaining = config.timePerQuestion
            while (remaining > 0) {
                delay(100)
                remaining -= 100

                val currentState = _gameState.value
                if (currentState is GameState.QuestionActive) {
                    _gameState.value = currentState.copy(
                        remainingTimeMs = remaining,
                        playerAnswered = playerAnswer != null,
                        botAnswered = botDecision != null
                    )
                }
            }

            // Time's up - process results
            processQuestionEnd()
        }
    }

    fun submitAnswer(answer: String) {
        if (playerAnswer != null) return

        val responseTime = currentTimeMillis() - questionStartTime
        val currentState = _gameState.value as? GameState.QuestionActive ?: return
        val isCorrect = answer == currentState.question.correctAnswer

        val points = scoringEngine.calculatePoints(
            isCorrect = isCorrect,
            responseTimeMs = responseTime,
            currentStreak = playerStreak
        )

        playerAnswer = PlayerAnswer(
            answer = answer,
            isCorrect = isCorrect,
            responseTimeMs = responseTime,
            pointsEarned = points
        )

        // Track stats
        if (isCorrect) {
            playerStreak++
            longestPlayerStreak = maxOf(longestPlayerStreak, playerStreak)
            fastestPlayerAnswer = minOf(fastestPlayerAnswer, responseTime)
        } else {
            playerStreak = 0
        }

        checkBothAnswered()
    }

    private fun checkBothAnswered() {
        if (playerAnswer != null && botDecision != null) {
            timerJob?.cancel()
            botAnswerJob?.cancel()
            scope.launch { processQuestionEnd() }
        }
    }

    private suspend fun processQuestionEnd() {
        // Prevent double processing
        if (isProcessingQuestionEnd) return
        isProcessingQuestionEnd = true

        timerJob?.cancel()
        botAnswerJob?.cancel()

        val currentState = _gameState.value as? GameState.QuestionActive
        if (currentState == null) {
            isProcessingQuestionEnd = false
            return
        }
        val question = currentState.question

        // Process player answer
        val finalPlayerAnswer = playerAnswer ?: PlayerAnswer(
            answer = null,
            isCorrect = false,
            responseTimeMs = config.timePerQuestion,
            pointsEarned = 0
        )

        // Process bot answer
        val finalBotDecision = botDecision ?: botBehavior?.makeDecision(question)
        val botPoints = if (finalBotDecision?.isCorrect == true) {
            scoringEngine.calculatePoints(
                isCorrect = true,
                responseTimeMs = finalBotDecision.responseTimeMs,
                currentStreak = botStreak
            )
        } else 0

        val botAnswer = PlayerAnswer(
            answer = finalBotDecision?.selectedAnswer,
            isCorrect = finalBotDecision?.isCorrect ?: false,
            responseTimeMs = finalBotDecision?.responseTimeMs ?: config.timePerQuestion,
            pointsEarned = botPoints
        )

        // Update scores
        playerRoundScore += finalPlayerAnswer.pointsEarned
        botRoundScore += botAnswer.pointsEarned
        playerTotalScore += finalPlayerAnswer.pointsEarned
        botTotalScore += botAnswer.pointsEarned

        if (finalPlayerAnswer.isCorrect) playerRoundCorrect++
        if (botAnswer.isCorrect) {
            botRoundCorrect++
            botStreak++
        } else {
            botStreak = 0
        }

        totalPlayerCorrect += if (finalPlayerAnswer.isCorrect) 1 else 0
        totalQuestions++

        // Show question result
        _gameState.value = GameState.QuestionResult(
            roundNumber = currentRound,
            questionNumber = currentQuestion + 1,
            question = question,
            playerAnswer = finalPlayerAnswer,
            botAnswer = botAnswer,
            playerTotalScore = playerRoundScore,
            botTotalScore = botRoundScore
        )

        // Schedule transition to next question/round
        resultTransitionJob?.cancel()
        resultTransitionJob = scope.launch {
            delay(2000)
            isProcessingQuestionEnd = false
            currentQuestion++
            if (currentQuestion < config.questionsPerRound) {
                startQuestion()
            } else {
                endRound()
            }
        }
    }

    private suspend fun endRound() {
        val winner = scoringEngine.determineRoundWinner(playerRoundScore, botRoundScore)

        when (winner) {
            Winner.PLAYER -> playerRoundsWon++
            Winner.BOT -> botRoundsWon++
            Winner.TIE -> {
                // Both get half credit for a tie
            }
        }

        val roundResult = RoundResult(
            roundNumber = currentRound,
            playerScore = playerRoundScore,
            botScore = botRoundScore,
            playerCorrect = playerRoundCorrect,
            botCorrect = botRoundCorrect,
            winner = winner
        )
        roundResults.add(roundResult)

        _gameState.value = GameState.RoundComplete(
            roundResult = roundResult,
            playerTotalScore = playerTotalScore,
            botTotalScore = botTotalScore,
            playerRoundsWon = playerRoundsWon,
            botRoundsWon = botRoundsWon
        )

        delay(3000)

        if (currentRound < config.totalRounds) {
            startRound()
        } else {
            endMatch()
        }
    }

    private fun endMatch() {
        val matchWinner = scoringEngine.determineMatchWinner(playerRoundsWon, botRoundsWon)

        val matchResult = MatchResult(
            playerTotalScore = playerTotalScore,
            botTotalScore = botTotalScore,
            playerRoundsWon = playerRoundsWon,
            botRoundsWon = botRoundsWon,
            winner = matchWinner,
            roundResults = roundResults.toList(),
            longestStreak = longestPlayerStreak,
            fastestAnswer = if (fastestPlayerAnswer == Long.MAX_VALUE) 0 else fastestPlayerAnswer,
            accuracy = scoringEngine.calculateAccuracy(totalPlayerCorrect, totalQuestions)
        )

        _gameState.value = GameState.MatchComplete(
            result = matchResult,
            difficulty = config.difficulty
        )
    }

    fun continueFromRoundComplete() {
        // This is handled automatically with delay
    }

    fun resetGame() {
        timerJob?.cancel()
        botAnswerJob?.cancel()
        resultTransitionJob?.cancel()
        resetMatchState()
        _gameState.value = GameState.Idle
    }

    private fun resetMatchState() {
        currentRound = 0
        currentQuestion = 0
        questions = emptyList()
        playerRoundScore = 0
        botRoundScore = 0
        playerRoundCorrect = 0
        botRoundCorrect = 0
        playerTotalScore = 0
        botTotalScore = 0
        playerRoundsWon = 0
        botRoundsWon = 0
        roundResults.clear()
        playerStreak = 0
        botStreak = 0
        longestPlayerStreak = 0
        fastestPlayerAnswer = Long.MAX_VALUE
        totalPlayerCorrect = 0
        totalQuestions = 0
        playerAnswer = null
        botDecision = null
        isProcessingQuestionEnd = false
    }

    private fun currentTimeMillis(): Long {
        return com.markduenas.antonymizer.util.currentTimeMillis()
    }
}
