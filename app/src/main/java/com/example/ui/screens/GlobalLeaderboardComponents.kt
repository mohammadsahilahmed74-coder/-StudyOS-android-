package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.focus.WeekKeyUtils
import com.example.data.focus.WeeklyFocusRanking

/**
 * Weekly Global Focus Leaderboard Card displayed on the Focus Screen.
 * Features Top 3 visual podium, user rank highlights, and quick preview.
 */
@Composable
fun GlobalFocusLeaderboardCard(
    rankings: List<WeeklyFocusRanking>,
    userRank: Int,
    weeklyMinutes: Int,
    onOpenFull: () -> Unit,
    modifier: Modifier = Modifier
) {
    val top1 = rankings.getOrNull(0)
    val top2 = rankings.getOrNull(1)
    val top3 = rankings.getOrNull(2)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("global_focus_leaderboard_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFEF3C7),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Global Focus Leaderboard",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Weekly Rankings • ${WeekKeyUtils.getWeekDisplayTitle()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = if (userRank > 0) "Rank #$userRank" else "Ranked",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // User Current Standing Highlight Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (userRank > 0) "#$userRank" else "-",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Your Weekly Focus",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val hours = weeklyMinutes / 60
                            val mins = weeklyMinutes % 60
                            Text(
                                text = if (hours > 0) "${hours}h ${mins}m focused" else "${mins}m focused",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (userRank in 2..10 && top1 != null) {
                        val higher = rankings.getOrNull(userRank - 2)
                        val diff = higher?.let { (it.totalFocusMinutes - weeklyMinutes).coerceAtLeast(1) } ?: 30
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFD1FAE5)
                        ) {
                            Text(
                                text = "+${diff}m to #${userRank - 1}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (userRank == 1) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF3C7)
                        ) {
                            Text(
                                text = "👑 #1 Leader",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Top 3 Podium View
            if (top1 != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // 2nd Place (Silver)
                    if (top2 != null) {
                        PodiumColumn(
                            ranking = top2,
                            badgeEmoji = "🥈",
                            badgeColor = Color(0xFF94A3B8),
                            pedestalHeight = 90,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    // 1st Place (Gold)
                    PodiumColumn(
                        ranking = top1,
                        badgeEmoji = "👑 🥇",
                        badgeColor = Color(0xFFF59E0B),
                        pedestalHeight = 115,
                        modifier = Modifier.weight(1f)
                    )

                    // 3rd Place (Bronze)
                    if (top3 != null) {
                        PodiumColumn(
                            ranking = top3,
                            badgeEmoji = "🥉",
                            badgeColor = Color(0xFFD97706),
                            pedestalHeight = 78,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Top 4 & 5 Preview rows
            val previewRest = rankings.filter { it.rank in 4..5 }
            if (previewRest.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                previewRest.forEach { entry ->
                    LeaderboardRowItem(entry = entry, compact = true)
                }
            }

            // View Full Leaderboard Button
            Button(
                onClick = onOpenFull,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("view_full_leaderboard_button")
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "View Full Weekly Leaderboard (${rankings.size} Students)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun PodiumColumn(
    ranking: WeeklyFocusRanking,
    badgeEmoji: String,
    badgeColor: Color,
    pedestalHeight: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar Circle with initial
        Surface(
            shape = CircleShape,
            color = if (ranking.isCurrentUser) MaterialTheme.colorScheme.primary else badgeColor.copy(alpha = 0.2f),
            border = BorderStroke(2.dp, badgeColor),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = ranking.displayName.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (ranking.isCurrentUser) Color.White else badgeColor
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (ranking.isCurrentUser) "You" else ranking.displayName.substringBefore(" "),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = ranking.formattedTime,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Pedestal
        Surface(
            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
            color = badgeColor.copy(alpha = 0.15f),
            modifier = Modifier
                .fillMaxWidth()
                .height(pedestalHeight.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = badgeEmoji, fontSize = 16.sp)
                Text(
                    text = "#${ranking.rank}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = badgeColor
                )
            }
        }
    }
}

@Composable
fun LeaderboardRowItem(
    entry: WeeklyFocusRanking,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val rankBadgeBg = when (entry.rank) {
        1 -> Color(0xFFFEF3C7)
        2 -> Color(0xFFF1F5F9)
        3 -> Color(0xFFFFEDD5)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val rankTextColor = when (entry.rank) {
        1 -> Color(0xFFB45309)
        2 -> Color(0xFF475569)
        3 -> Color(0xFFC2410C)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (entry.isCurrentUser) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent,
        border = if (entry.isCurrentUser) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = if (compact) 2.dp else 4.dp)
            .testTag("leaderboard_item_${entry.rank}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = if (compact) 4.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Rank Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = rankBadgeBg,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "#${entry.rank}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = rankTextColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Avatar
                Surface(
                    shape = CircleShape,
                    color = if (entry.isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = entry.displayName.take(1).uppercase(),
                            color = if (entry.isCurrentUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (entry.isCurrentUser) "${entry.displayName} (You)" else entry.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (entry.isCurrentUser) FontWeight.Bold else FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = entry.studyTrack,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }

            // Stats (Hours, Streak)
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = entry.formattedTime,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (entry.isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🔥 ${entry.dailyStreak}d",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFD97706),
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "• ${entry.sessionsCompleted} sess",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

/**
 * Full Global Leaderboard Modal Dialog with search, rankings table, and live reload.
 */
@Composable
fun GlobalLeaderboardModalDialog(
    rankings: List<WeeklyFocusRanking>,
    userRank: Int,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(rankings, searchQuery) {
        if (searchQuery.isBlank()) {
            rankings
        } else {
            rankings.filter {
                it.displayName.contains(searchQuery, ignoreCase = true) ||
                        it.studyTrack.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val currentUserEntry = rankings.firstOrNull { it.isCurrentUser }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Weekly Leaderboard", fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onRefresh, modifier = Modifier.testTag("leaderboard_refresh_button")) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(20.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Search Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by student or major...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("leaderboard_search_input")
                )

                // Sticky Current User Standing Card
                if (currentUserEntry != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth().testTag("leaderboard_user_sticky_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("#${currentUserEntry.rank}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("You (${currentUserEntry.displayName})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text("${currentUserEntry.formattedTime} focused this week", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                            Text("🔥 ${currentUserEntry.dailyStreak}d", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                        }
                    }
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }

                // Scrollable Rankings List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("leaderboard_list"),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filtered, key = { it.userId }) { entry ->
                        LeaderboardRowItem(entry = entry)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.testTag("leaderboard_close_button")) {
                Text("Close")
            }
        }
    )
}
