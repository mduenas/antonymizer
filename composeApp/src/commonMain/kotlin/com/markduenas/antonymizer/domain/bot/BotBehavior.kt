package com.markduenas.antonymizer.domain.bot

import com.markduenas.antonymizer.data.model.BotDifficulty
import com.markduenas.antonymizer.domain.game.Question
import kotlin.random.Random

data class BotDecision(
    val selectedAnswer: String,
    val responseTimeMs: Long,
    val isCorrect: Boolean
)

class BotBehavior(
    private val difficulty: BotDifficulty
) {
    private val random = Random

    fun makeDecision(question: Question): BotDecision {
        val willBeCorrect = decideIfCorrect()
        val responseTime = calculateResponseTime()

        val selectedAnswer = if (willBeCorrect) {
            question.correctAnswer
        } else {
            selectWrongAnswer(question)
        }

        return BotDecision(
            selectedAnswer = selectedAnswer,
            responseTimeMs = responseTime,
            isCorrect = willBeCorrect
        )
    }

    private fun decideIfCorrect(): Boolean {
        val (minAccuracy, maxAccuracy) = difficulty.accuracyRange.start to difficulty.accuracyRange.endInclusive
        val accuracy = random.nextFloat() * (maxAccuracy - minAccuracy) + minAccuracy
        return random.nextFloat() < accuracy
    }

    private fun calculateResponseTime(): Long {
        val (minTime, maxTime) = difficulty.responseTimeRange.first to difficulty.responseTimeRange.last
        return random.nextLong(minTime, maxTime + 1)
    }

    private fun selectWrongAnswer(question: Question): String {
        val wrongAnswers = question.options.filter { it != question.correctAnswer }
        return if (wrongAnswers.isNotEmpty()) {
            wrongAnswers.random(random)
        } else {
            question.options.first()
        }
    }

    companion object {
        fun getBotName(difficulty: BotDifficulty): String {
            return when (difficulty) {
                BotDifficulty.EASY -> "Rookie Bot"
                BotDifficulty.MEDIUM -> "Challenger Bot"
                BotDifficulty.HARD -> "Expert Bot"
                BotDifficulty.EXPERT -> "Master Bot"
            }
        }

        fun getBotDescription(difficulty: BotDifficulty): String {
            return when (difficulty) {
                BotDifficulty.EASY -> "A beginner opponent. Good for practice!"
                BotDifficulty.MEDIUM -> "A balanced opponent. A fair challenge."
                BotDifficulty.HARD -> "A skilled opponent. Not easy to beat!"
                BotDifficulty.EXPERT -> "The ultimate challenge. Only the best can win!"
            }
        }
    }
}
