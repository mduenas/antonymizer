package com.markduenas.antonymizer.ui.screens.leaderboard

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
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
import com.markduenas.antonymizer.data.model.BotDifficulty
import com.markduenas.antonymizer.data.model.LeaderboardEntry
import com.markduenas.antonymizer.data.model.Rank
import com.markduenas.antonymizer.ui.theme.RankBronze
import com.markduenas.antonymizer.ui.theme.RankDiamond
import com.markduenas.antonymizer.ui.theme.RankGold
import com.markduenas.antonymizer.ui.theme.RankGrandmaster
import com.markduenas.antonymizer.ui.theme.RankMaster
import com.markduenas.antonymizer.ui.theme.RankPlatinum
import com.markduenas.antonymizer.ui.theme.RankSilver
import com.markduenas.antonymizer.ui.theme.StreakBronze
import com.markduenas.antonymizer.ui.theme.StreakGold
import com.markduenas.antonymizer.ui.theme.StreakSilver

@Composable
fun LeaderboardScreen(
    entries: List<LeaderboardEntry>,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("All") + BotDifficulty.entries.map { it.displayName }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBackClick) {
                Text("<  Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Leaderboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tabs for difficulty filter
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 16.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filter entries
        val filteredEntries = if (selectedTab == 0) {
            entries
        } else {
            val difficulty = BotDifficulty.entries[selectedTab - 1]
            entries.filter { it.difficulty == difficulty }
        }

        if (filteredEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No scores yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Play some games to see your scores here!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(filteredEntries) { index, entry ->
                    LeaderboardEntryCard(
                        position = index + 1,
                        entry = entry
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun LeaderboardEntryCard(
    position: Int,
    entry: LeaderboardEntry
) {
    val isTopThree = position <= 3

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isTopThree) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Position badge
            PositionBadge(position = position)

            Spacer(modifier = Modifier.width(12.dp))

            // Player info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = entry.playerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RankBadge(rank = entry.rank)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = entry.rank.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " | ${entry.difficulty.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Score
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${entry.score}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${entry.roundsWon}/${entry.totalRounds} rounds",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PositionBadge(position: Int) {
    val backgroundColor = when (position) {
        1 -> StreakGold
        2 -> StreakSilver
        3 -> StreakBronze
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = when (position) {
        1, 2, 3 -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .background(color = backgroundColor, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$position",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
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
            .size(16.dp)
            .background(color = color, shape = CircleShape)
    )
}
