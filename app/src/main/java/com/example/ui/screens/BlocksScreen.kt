package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.AppBlockerManager
import com.example.util.WebsiteBlockerManager
import com.example.util.YouTubeShortsBlockerManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlocksScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isAppBlockerEnabled by remember { mutableStateOf(AppBlockerManager.isAppBlockerEnabled(context)) }
    var isWebsiteBlockerEnabled by remember { mutableStateOf(WebsiteBlockerManager.isWebsiteBlockingActive(context)) }
    var shortsStatus by remember { mutableStateOf(YouTubeShortsBlockerManager.getStatus(context)) }
    
    var showAppLimitsDialog by remember { mutableStateOf(false) }
    var showShortsSettingsDialog by remember { mutableStateOf(false) }
    var showWebsiteDialog by remember { mutableStateOf(false) }
    var showStrictModeDialog by remember { mutableStateOf(false) }
    var strictModeEnabled by remember { mutableStateOf(false) }

    val blockedAppsCount = remember { AppBlockerManager.getBlockedPackages(context).size }
    val blockedWebsitesCount = remember { WebsiteBlockerManager.getBlockedWebsites(context).size }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("blocks_screen_root"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header v2.4
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Blocks & Wellbeing v2.4",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Regain control over digital distractions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "v2.4 Pro",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        // Active Status Overview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = Color.White)
                    }
                    Column {
                        Text(text = "Shields Active", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(text = "$blockedAppsCount Apps • $blockedWebsitesCount Websites Protected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(
                    checked = isAppBlockerEnabled || isWebsiteBlockerEnabled || shortsStatus.isEnabled,
                    onCheckedChange = { enabled ->
                        AppBlockerManager.setAppBlockerEnabled(context, enabled)
                        WebsiteBlockerManager.setWebsiteBlockingActive(context, enabled)
                        YouTubeShortsBlockerManager.setBlockerEnabled(context, enabled)
                        isAppBlockerEnabled = enabled
                        isWebsiteBlockerEnabled = enabled
                        shortsStatus = YouTubeShortsBlockerManager.getStatus(context)
                    }
                )
            }
        }

        // SECTION 1: APP LIMITS
        Text(text = "App Limits & Usage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showAppLimitsDialog = true },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(text = "App Limits Configuration", fontWeight = FontWeight.SemiBold)
                        Text(text = "$blockedAppsCount apps currently restricted with daily limits", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // SECTION 2: BLOCK SHORTS (YouTube Shorts & Instagram Reels)
        Text(text = "Block Shorts & Reels", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // YouTube Shorts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFFEF4444))
                        Column {
                            Text(text = "YouTube Shorts Blocker", fontWeight = FontWeight.Bold)
                            Text(text = "Blocks vertical video reels while keeping normal videos accessible", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = shortsStatus.isEnabled,
                        onCheckedChange = { enabled ->
                            YouTubeShortsBlockerManager.setBlockerEnabled(context, enabled)
                            shortsStatus = YouTubeShortsBlockerManager.getStatus(context)
                        }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // First Short Allowed
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                        Column {
                            Text(text = "First Short Allowed", fontWeight = FontWeight.Bold)
                            Text(text = "Allow first short; block starting from the second short", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = shortsStatus.isFirstShortAllowed,
                        onCheckedChange = { enabled ->
                            YouTubeShortsBlockerManager.setFirstShortAllowedEnabled(context, enabled)
                            shortsStatus = YouTubeShortsBlockerManager.getStatus(context)
                        }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Instagram Reels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color(0xFFEC4899))
                        Column {
                            Text(text = "Instagram Reels Blocker", fontWeight = FontWeight.Bold)
                            Text(text = "Interception guard against endless Instagram scrolling", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = isAppBlockerEnabled,
                        onCheckedChange = { enabled ->
                            AppBlockerManager.setAppBlockerEnabled(context, enabled)
                            isAppBlockerEnabled = enabled
                        }
                    )
                }
            }
        }

        // SECTION 3: OTHER BLOCKS (Website Blocker & YouTube Study Mode)
        Text(text = "Other Blocks & Focus Shields", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Website Blocker
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showWebsiteDialog = true },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF6366F1))
                        Column {
                            Text(text = "Website Blocker", fontWeight = FontWeight.Bold)
                            Text(text = "$blockedWebsitesCount domains blocked 24/7", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = isWebsiteBlockerEnabled,
                        onCheckedChange = { enabled ->
                            WebsiteBlockerManager.setWebsiteBlockingActive(context, enabled)
                            isWebsiteBlockerEnabled = enabled
                        }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // YouTube Study Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.School, contentDescription = null, tint = Color(0xFF3B82F6))
                        Column {
                            Text(text = "YouTube Study Mode", fontWeight = FontWeight.Bold)
                            Text(text = "Hide comments, recommendations & sidebar distractions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = true,
                        onCheckedChange = {}
                    )
                }
            }
        }

        // SECTION 4: STRICT MODE
        Text(text = "Security & Strict Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showStrictModeDialog = true },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFF59E0B))
                    Column {
                        Text(text = "Strict Mode", fontWeight = FontWeight.Bold)
                        Text(text = if (strictModeEnabled) "Active: Blockers cannot be disabled" else "Disabled: Tap to configure strict locking", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(
                    checked = strictModeEnabled,
                    onCheckedChange = { strictModeEnabled = it }
                )
            }
        }
    }

    // App Limits Dialog / Bottom Sheet
    if (showAppLimitsDialog) {
        AlertDialog(
            onDismissRequest = { showAppLimitsDialog = false },
            title = { Text("App Limits & Blocking", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select apps to restrict and set daily usage limits to prevent doomscrolling.")
                    Button(onClick = {
                        AppBlockerManager.requestUsageStatsPermission(context)
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Grant Usage Stats Permission")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppLimitsDialog = false }) { Text("Done") }
            }
        )
    }

    // Strict Mode Dialog
    if (showStrictModeDialog) {
        AlertDialog(
            onDismissRequest = { showStrictModeDialog = false },
            title = { Text("Strict Mode v2.4", fontWeight = FontWeight.Bold) },
            text = {
                Text("Strict Mode prevents you from turning off blockers during active study sessions or scheduled deep work hours. This ensures maximum academic discipline.")
            },
            confirmButton = {
                Button(onClick = {
                    strictModeEnabled = true
                    showStrictModeDialog = false
                }) { Text("Enable Strict Mode") }
            },
            dismissButton = {
                TextButton(onClick = { showStrictModeDialog = false }) { Text("Cancel") }
            }
        )
    }
}
