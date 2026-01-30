package com.markduenas.antonymizer.ui.screens.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markduenas.antonymizer.data.model.BotDifficulty
import com.markduenas.antonymizer.data.model.PlayerStats
import com.markduenas.antonymizer.data.model.Rank
import com.markduenas.antonymizer.domain.bot.BotBehavior
import com.markduenas.antonymizer.ui.components.PrimaryButton
import com.markduenas.antonymizer.ui.components.SecondaryButton
import com.markduenas.antonymizer.ui.theme.RankBronze
import com.markduenas.antonymizer.ui.theme.RankDiamond
import com.markduenas.antonymizer.ui.theme.RankGold
import com.markduenas.antonymizer.ui.theme.RankGrandmaster
import com.markduenas.antonymizer.ui.theme.RankMaster
import com.markduenas.antonymizer.ui.theme.RankPlatinum
import com.markduenas.antonymizer.ui.theme.RankSilver

@Composable
fun HomeScreen(
    playerStats: PlayerStats,
    playerName: String,
    onPlayClick: (BotDifficulty) -> Unit,
    onLeaderboardClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedDifficulty by remember { mutableStateOf(BotDifficulty.MEDIUM) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header with settings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hello, $playerName",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onSettingsClick) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Game title
        Text(
            text = "ANTONYM",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "ARENA",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Player stats card
        PlayerStatsCard(
            stats = playerStats,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Difficulty selection
        Text(
            text = "Select Difficulty",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        DifficultySelector(
            selectedDifficulty = selectedDifficulty,
            onDifficultySelected = { selectedDifficulty = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Play button
        PrimaryButton(
            text = "PLAY",
            onClick = { onPlayClick(selectedDifficulty) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Leaderboard button
        SecondaryButton(
            text = "Leaderboard",
            onClick = onLeaderboardClick,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun PlayerStatsCard(
    stats: PlayerStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Your Rank",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RankBadge(rank = stats.rank)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stats.rank.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Total Points",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${stats.totalPoints}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Games",
                    value = "${stats.totalGamesPlayed}"
                )
                StatItem(
                    label = "Wins",
                    value = "${stats.totalWins}"
                )
                StatItem(
                    label = "Win Rate",
                    value = "${(stats.winRate * 100).toInt()}%"
                )
                StatItem(
                    label = "Best Streak",
                    value = "${stats.longestStreak}"
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RankBadge(
    rank: Rank,
    modifier: Modifier = Modifier
) {
    val color = when (rank) {
        Rank.BRONZE -> RankBronze
        Rank.SILVER -> RankSilver
        Rank.GOLD -> RankGold
        Rank.PLATINUM -> RankPlatinum
        Rank.DIAMOND -> RankDiamond
        Rank.MASTER -> RankMaster
        Rank.GRANDMASTER -> RankGrandmaster
    }

    Box(
        modifier = modifier
            .size(32.dp)
            .background(color = color, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = rank.tier.toString(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun DifficultySelector(
    selectedDifficulty: BotDifficulty,
    onDifficultySelected: (BotDifficulty) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BotDifficulty.entries.forEach { difficulty ->
            DifficultyCard(
                difficulty = difficulty,
                isSelected = difficulty == selectedDifficulty,
                onClick = { onDifficultySelected(difficulty) }
            )
        }
    }
}

@Composable
private fun DifficultyCard(
    difficulty: BotDifficulty,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = BotBehavior.getBotName(difficulty),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = BotBehavior.getBotDescription(difficulty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Text(
                    text = ">",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
