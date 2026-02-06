package com.markduenas.antonymizer.ui.screens.game

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.markduenas.antonymizer.domain.bot.BotBehavior
import com.markduenas.antonymizer.domain.game.GameState
import com.markduenas.antonymizer.domain.game.MatchResult
import com.markduenas.antonymizer.domain.game.PlayerAnswer
import com.markduenas.antonymizer.domain.game.Winner
import com.markduenas.antonymizer.ui.components.AnswerButton
import com.markduenas.antonymizer.ui.components.AnswerButtonState
import com.markduenas.antonymizer.ui.components.CountdownDisplay
import com.markduenas.antonymizer.ui.components.LoadingIndicator
import com.markduenas.antonymizer.ui.components.PrimaryButton
import com.markduenas.antonymizer.ui.components.QuestionProgress
import com.markduenas.antonymizer.ui.components.RoundIndicator
import com.markduenas.antonymizer.ui.components.ScoreDisplay
import com.markduenas.antonymizer.ui.components.StreakIndicator
import com.markduenas.antonymizer.ui.components.TimerBar
import com.markduenas.antonymizer.ui.components.WordCard
import com.markduenas.antonymizer.ui.theme.CorrectGreen
import com.markduenas.antonymizer.ui.theme.IncorrectRed

@Composable
fun GameScreen(
    gameState: GameState,
    rewardedAdReady: Boolean = false,
    onAnswerSelected: (String) -> Unit,
    onStartMatch: () -> Unit,
    onExitGame: () -> Unit,
    onHintRequested: (onRewarded: () -> Unit, onDismissed: () -> Unit) -> Unit = { _, onDismissed -> onDismissed() },
    onMatchComplete: (MatchResult) -> Unit,
    modifier: Modifier = Modifier
) {
    // Track hints used per match (max 2)
    var hintsUsedInMatch by remember { mutableStateOf(0) }
    var eliminatedOptions by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Reset hints when match restarts
    val currentMatchState = gameState
    if (currentMatchState is GameState.MatchReady) {
        hintsUsedInMatch = 0
        eliminatedOptions = emptySet()
    }
    // Reset eliminated options when question changes
    if (currentMatchState is GameState.QuestionActive) {
        val questionKey = "${currentMatchState.roundNumber}-${currentMatchState.questionNumber}"
        val lastQuestionKey = remember { mutableStateOf("") }
        if (lastQuestionKey.value != questionKey) {
            eliminatedOptions = emptySet()
            lastQuestionKey.value = questionKey
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        AnimatedContent(
            targetState = gameState,
            transitionSpec = { fadeIn() togetherWith fadeOut() }
        ) { state ->
            when (state) {
                is GameState.Idle -> {
                    LoadingIndicator(message = "Preparing...")
                }

                is GameState.Loading -> {
                    LoadingIndicator(message = state.message)
                }

                is GameState.MatchReady -> {
                    MatchReadyContent(
                        difficulty = state.difficulty,
                        playerName = state.playerName,
                        onStartMatch = onStartMatch,
                        onExit = onExitGame
                    )
                }

                is GameState.RoundStarting -> {
                    RoundStartingContent(
                        roundNumber = state.roundNumber,
                        totalRounds = state.totalRounds,
                        countdown = state.countdown
                    )
                }

                is GameState.QuestionActive -> {
                    QuestionActiveContent(
                        state = state,
                        onAnswerSelected = onAnswerSelected,
                        onExit = onExitGame,
                        hintsUsedInMatch = hintsUsedInMatch,
                        eliminatedOptions = eliminatedOptions,
                        rewardedAdReady = rewardedAdReady,
                        onHintRequested = {
                            onHintRequested(
                                {
                                    // Hint granted - eliminate 2 wrong answers
                                    hintsUsedInMatch++
                                    val wrongOptions = state.question.options
                                        .filter { it != state.question.correctAnswer }
                                        .shuffled()
                                        .take(2)
                                    eliminatedOptions = wrongOptions.toSet()
                                },
                                {
                                    // Ad dismissed early or failed
                                }
                            )
                        }
                    )
                }

                is GameState.QuestionResult -> {
                    QuestionResultContent(state = state)
                }

                is GameState.RoundComplete -> {
                    RoundCompleteContent(state = state)
                }

                is GameState.MatchComplete -> {
                    onMatchComplete(state.result)
                }

                is GameState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = onExitGame
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchReadyContent(
    difficulty: com.markduenas.antonymizer.data.model.BotDifficulty,
    playerName: String,
    onStartMatch: () -> Unit,
    onExit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Match Ready",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = playerName,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "VS",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = BotBehavior.getBotName(difficulty),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Best of 5 rounds",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "10 words per round",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "5 seconds per word",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        PrimaryButton(
            text = "START MATCH",
            onClick = onStartMatch,
            modifier = Modifier.fillMaxWidth(0.8f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onExit) {
            Text("Exit")
        }
    }
}

@Composable
private fun RoundStartingContent(
    roundNumber: Int,
    totalRounds: Int,
    countdown: Int
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Round $roundNumber of $totalRounds",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(48.dp))

        CountdownDisplay(count = countdown)
    }
}

@Composable
private fun QuestionActiveContent(
    state: GameState.QuestionActive,
    onAnswerSelected: (String) -> Unit,
    onExit: () -> Unit,
    hintsUsedInMatch: Int = 0,
    eliminatedOptions: Set<String> = emptySet(),
    rewardedAdReady: Boolean = false,
    onHintRequested: () -> Unit = {}
) {
    var selectedAnswer by remember(state.questionNumber) { mutableStateOf<String?>(null) }
    val canUseHint = hintsUsedInMatch < 2 && rewardedAdReady && !state.playerAnswered && eliminatedOptions.isEmpty()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundIndicator(
                currentRound = state.roundNumber,
                totalRounds = 5
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hint button
                if (canUseHint) {
                    TextButton(onClick = onHintRequested) {
                        Text("Hint (${2 - hintsUsedInMatch})")
                    }
                }
                TextButton(onClick = onExit) {
                    Text("Exit")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Question progress
        QuestionProgress(
            currentQuestion = state.questionNumber,
            totalQuestions = state.totalQuestions
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Scores
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ScoreDisplay(
                label = "You",
                score = state.playerScore,
                isPlayer = true
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (state.playerStreak >= 2) {
                    StreakIndicator(streak = state.playerStreak)
                } else {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            ScoreDisplay(
                label = "Bot",
                score = state.botScore,
                isPlayer = false
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Timer
        TimerBar(
            remainingTimeMs = state.remainingTimeMs,
            totalTimeMs = state.question.timeLimit
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Word card
        WordCard(word = state.question.word)

        Spacer(modifier = Modifier.height(24.dp))

        // Answer options
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            state.question.options.forEach { option ->
                val isEliminated = eliminatedOptions.contains(option)
                val buttonState = when {
                    isEliminated -> AnswerButtonState.ELIMINATED
                    state.playerAnswered && selectedAnswer == option -> AnswerButtonState.SELECTED
                    state.playerAnswered -> AnswerButtonState.DISABLED
                    else -> AnswerButtonState.DEFAULT
                }

                AnswerButton(
                    text = option,
                    state = buttonState,
                    onClick = {
                        if (!state.playerAnswered && !isEliminated) {
                            selectedAnswer = option
                            onAnswerSelected(option)
                        }
                    },
                    enabled = !state.playerAnswered && !isEliminated
                )
            }
        }
    }
}

@Composable
private fun QuestionResultContent(
    state: GameState.QuestionResult
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Show the word
        Text(
            text = state.question.word,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Correct answer: ${state.question.correctAnswer}",
            style = MaterialTheme.typography.titleMedium,
            color = CorrectGreen
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Results
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ResultColumn(
                label = "You",
                answer = state.playerAnswer,
                isPlayer = true
            )
            ResultColumn(
                label = "Bot",
                answer = state.botAnswer,
                isPlayer = false
            )
        }
    }
}

@Composable
private fun ResultColumn(
    label: String,
    answer: PlayerAnswer,
    isPlayer: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = answer.answer ?: "No answer",
            style = MaterialTheme.typography.bodyLarge,
            color = if (answer.isCorrect) CorrectGreen else IncorrectRed
        )
        Text(
            text = if (answer.isCorrect) "+${answer.pointsEarned}" else "0",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (answer.isCorrect) CorrectGreen else MaterialTheme.colorScheme.outline
        )
        Text(
            text = "${answer.responseTimeMs / 1000.0}s",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RoundCompleteContent(
    state: GameState.RoundComplete
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Round ${state.roundResult.roundNumber} Complete",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        val winnerText = when (state.roundResult.winner) {
            Winner.PLAYER -> "You won this round!"
            Winner.BOT -> "Bot won this round"
            Winner.TIE -> "It's a tie!"
        }
        val winnerColor = when (state.roundResult.winner) {
            Winner.PLAYER -> CorrectGreen
            Winner.BOT -> IncorrectRed
            Winner.TIE -> MaterialTheme.colorScheme.outline
        }

        Text(
            text = winnerText,
            style = MaterialTheme.typography.titleLarge,
            color = winnerColor
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "You",
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = "${state.roundResult.playerScore}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${state.roundResult.playerCorrect}/10 correct",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Bot",
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = "${state.roundResult.botScore}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${state.roundResult.botCorrect}/10 correct",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Match Score",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "${state.playerRoundsWon} - ${state.botRoundsWon}",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Oops!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        PrimaryButton(
            text = "Go Back",
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(0.6f)
        )
    }
}
