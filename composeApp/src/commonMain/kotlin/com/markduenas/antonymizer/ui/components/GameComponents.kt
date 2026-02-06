package com.markduenas.antonymizer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.markduenas.antonymizer.ui.theme.CorrectGreen
import com.markduenas.antonymizer.ui.theme.IncorrectRed
import com.markduenas.antonymizer.ui.theme.StreakFire
import com.markduenas.antonymizer.ui.theme.StreakGold
import com.markduenas.antonymizer.ui.theme.TimerCritical
import com.markduenas.antonymizer.ui.theme.TimerFull
import com.markduenas.antonymizer.ui.theme.TimerLow
import com.markduenas.antonymizer.ui.theme.TimerMedium

@Composable
fun WordCard(
    word: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Find the antonym of:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = word.uppercase(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

enum class AnswerButtonState {
    DEFAULT,
    SELECTED,
    CORRECT,
    INCORRECT,
    DISABLED,
    ELIMINATED  // Crossed out by hint
}

@Composable
fun AnswerButton(
    text: String,
    state: AnswerButtonState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val backgroundColor by animateColorAsState(
        targetValue = when (state) {
            AnswerButtonState.DEFAULT -> MaterialTheme.colorScheme.surface
            AnswerButtonState.SELECTED -> MaterialTheme.colorScheme.primaryContainer
            AnswerButtonState.CORRECT -> CorrectGreen
            AnswerButtonState.INCORRECT -> IncorrectRed
            AnswerButtonState.DISABLED -> MaterialTheme.colorScheme.surfaceVariant
            AnswerButtonState.ELIMINATED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        },
        animationSpec = tween(200)
    )

    val borderColor by animateColorAsState(
        targetValue = when (state) {
            AnswerButtonState.DEFAULT -> MaterialTheme.colorScheme.outline
            AnswerButtonState.SELECTED -> MaterialTheme.colorScheme.primary
            AnswerButtonState.CORRECT -> CorrectGreen
            AnswerButtonState.INCORRECT -> IncorrectRed
            AnswerButtonState.DISABLED -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            AnswerButtonState.ELIMINATED -> IncorrectRed.copy(alpha = 0.5f)
        },
        animationSpec = tween(200)
    )

    val textColor = when (state) {
        AnswerButtonState.CORRECT, AnswerButtonState.INCORRECT -> Color.White
        AnswerButtonState.DISABLED -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        AnswerButtonState.ELIMINATED -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    val isClickable = enabled && state == AnswerButtonState.DEFAULT
    val textDecoration = if (state == AnswerButtonState.ELIMINATED) TextDecoration.LineThrough else null

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(enabled = isClickable) { onClick() },
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = textColor,
                textDecoration = textDecoration
            )
        }
    }
}

@Composable
fun TimerBar(
    remainingTimeMs: Long,
    totalTimeMs: Long,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = (remainingTimeMs.toFloat() / totalTimeMs).coerceIn(0f, 1f),
        animationSpec = tween(100)
    )

    val timerColor by animateColorAsState(
        targetValue = when {
            progress > 0.6f -> TimerFull
            progress > 0.3f -> TimerMedium
            progress > 0.15f -> TimerLow
            else -> TimerCritical
        },
        animationSpec = tween(300)
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Time",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${(remainingTimeMs / 100) / 10.0}s",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = timerColor
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = timerColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun ScoreDisplay(
    label: String,
    score: Int,
    isPlayer: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isPlayer) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.secondary
            }
        )
        AnimatedCounter(
            count = score,
            style = MaterialTheme.typography.headlineSmall
        )
    }
}

@Composable
fun StreakIndicator(
    streak: Int,
    modifier: Modifier = Modifier
) {
    if (streak < 2) return

    val streakColor = when {
        streak >= 5 -> StreakFire
        streak >= 3 -> StreakGold
        else -> MaterialTheme.colorScheme.tertiary
    }

    Row(
        modifier = modifier
            .background(
                color = streakColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${streak}x",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = streakColor
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "STREAK",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = streakColor
        )
    }
}

@Composable
fun RoundIndicator(
    currentRound: Int,
    totalRounds: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalRounds) { index ->
            val isActive = index < currentRound
            val isCurrent = index == currentRound - 1

            Box(
                modifier = Modifier
                    .size(if (isCurrent) 12.dp else 8.dp)
                    .background(
                        color = when {
                            isCurrent -> MaterialTheme.colorScheme.primary
                            isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
fun QuestionProgress(
    currentQuestion: Int,
    totalQuestions: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Question",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$currentQuestion / $totalQuestions",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { currentQuestion.toFloat() / totalQuestions },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
