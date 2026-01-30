package com.markduenas.antonymizer.ui.screens.results

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markduenas.antonymizer.data.model.BotDifficulty
import com.markduenas.antonymizer.domain.game.MatchResult
import com.markduenas.antonymizer.domain.game.RoundResult
import com.markduenas.antonymizer.domain.game.Winner
import com.markduenas.antonymizer.ui.components.PrimaryButton
import com.markduenas.antonymizer.ui.components.SecondaryButton
import com.markduenas.antonymizer.ui.theme.CorrectGreen
import com.markduenas.antonymizer.ui.theme.IncorrectRed
import com.markduenas.antonymizer.ui.theme.StreakGold

@Composable
fun ResultsScreen(
    result: MatchResult,
    difficulty: BotDifficulty,
    onPlayAgain: () -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Victory/Defeat header
        val headerColor = when (result.winner) {
            Winner.PLAYER -> CorrectGreen
            Winner.BOT -> IncorrectRed
            Winner.TIE -> MaterialTheme.colorScheme.primary
        }
        val headerText = when (result.winner) {
            Winner.PLAYER -> "VICTORY!"
            Winner.BOT -> "DEFEAT"
            Winner.TIE -> "TIE MATCH"
        }

        Text(
            text = headerText,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = headerColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${result.playerRoundsWon} - ${result.botRoundsWon}",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Score summary card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Your Score",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${result.playerTotalScore}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Bot Score",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${result.botTotalScore}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Stats highlights
        Text(
            text = "Match Stats",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                StatRow(
                    label = "Accuracy",
                    value = "${(result.accuracy * 100).toInt()}%"
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                StatRow(
                    label = "Longest Streak",
                    value = "${result.longestStreak}x",
                    highlight = result.longestStreak >= 5
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                StatRow(
                    label = "Fastest Answer",
                    value = if (result.fastestAnswer > 0) "${result.fastestAnswer / 1000.0}s" else "-"
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                StatRow(
                    label = "Difficulty",
                    value = difficulty.displayName
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Round breakdown
        Text(
            text = "Round Breakdown",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        result.roundResults.forEach { round ->
            RoundResultCard(round = round)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Action buttons
        PrimaryButton(
            text = "Play Again",
            onClick = onPlayAgain,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        SecondaryButton(
            text = "Back to Home",
            onClick = onGoHome,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (highlight) StreakGold else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun RoundResultCard(
    round: RoundResult
) {
    val backgroundColor = when (round.winner) {
        Winner.PLAYER -> CorrectGreen.copy(alpha = 0.1f)
        Winner.BOT -> IncorrectRed.copy(alpha = 0.1f)
        Winner.TIE -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Round ${round.roundNumber}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "You",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "${round.playerScore}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = when (round.winner) {
                        Winner.PLAYER -> "W"
                        Winner.BOT -> "L"
                        Winner.TIE -> "T"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (round.winner) {
                        Winner.PLAYER -> CorrectGreen
                        Winner.BOT -> IncorrectRed
                        Winner.TIE -> MaterialTheme.colorScheme.outline
                    }
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Bot",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "${round.botScore}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
