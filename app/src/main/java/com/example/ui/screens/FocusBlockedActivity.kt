package com.example.ui.screens

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainActivity
import com.example.ui.theme.StudyOSTheme
import com.example.util.YouTubeShortsBlockerManager

class FocusBlockedActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isShorts = intent.getBooleanExtra("IS_SHORTS", false)
        val isWebsite = intent.getBooleanExtra("IS_WEBSITE", false)
        val blockedDomain = intent.getStringExtra("BLOCKED_DOMAIN") ?: ""
        val blockedPkg = intent.getStringExtra("BLOCKED_PACKAGE") ?: ""

        val blockedItemName = if (isShorts) {
            "YouTube Shorts"
        } else if (isWebsite) {
            blockedDomain
        } else {
            try {
                val appInfo = packageManager.getApplicationInfo(blockedPkg, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                "This App"
            }
        }

        setContent {
            StudyOSTheme(darkTheme = true) {
                FocusBlockedScreen(
                    itemName = blockedItemName,
                    isWebsite = isWebsite,
                    isShorts = isShorts,
                    onReturnToFocus = {
                        val mainIntent = Intent(this@FocusBlockedActivity, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(mainIntent)
                        finish()
                    },
                    onTemporaryPause = {
                        YouTubeShortsBlockerManager.temporarilyDisable(this@FocusBlockedActivity, 15)
                        finish()
                    }
                )
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(mainIntent)
        finish()
    }
}

@Composable
fun FocusBlockedScreen(
    itemName: String,
    isWebsite: Boolean = false,
    isShorts: Boolean = false,
    onReturnToFocus: () -> Unit,
    onTemporaryPause: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(24.dp)
            .testTag("focus_blocked_screen"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isShorts) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isShorts) Icons.Default.PlayCircleOutline else Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Text(
                    text = if (isShorts) "YouTube Shorts Blocked" else if (isWebsite) "Website Blocked" else "App Blocked",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = if (isShorts) {
                        "StudyOS blocks addictive short-form video feeds to protect your dopamine baseline and attention span.\n\n✨ Normal educational YouTube videos, playlists, and tutorials remain 100% accessible."
                    } else if (isWebsite) {
                        "\"$itemName\" is on your focus blocklist and is restricted during active focus sessions."
                    } else {
                        "$itemName is currently blocked because you are in an active focus session."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = Color.White.copy(alpha = 0.85f),
                    lineHeight = 22.sp
                )

                Text(
                    text = if (isShorts) "Choose intentional learning over infinite scrolling." else "Honor your commitment and return to your study flow!",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onReturnToFocus,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("return_to_focus_button")
                ) {
                    Icon(Icons.Default.HourglassEmpty, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Return to StudyOS", fontWeight = FontWeight.Bold)
                }

                if (isShorts) {
                    OutlinedButton(
                        onClick = onTemporaryPause,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("shorts_temp_pause_button")
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF94A3B8))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pause Shorts Blocker for 15 min", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
