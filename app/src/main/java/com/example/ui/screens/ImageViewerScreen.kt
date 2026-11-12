package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.library.StudyLibraryStorageManager
import com.example.data.model.NoteAttachment
import java.io.File

@Composable
fun ImageViewerModal(
    attachment: NoteAttachment,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val localFile = remember(attachment.localPath) { File(attachment.localPath) }
    val isLocalAvailable = localFile.exists()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020617)) // Deepest Obsidian Dark
            .statusBarsPadding()
            .testTag("image_viewer_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF0F172A),
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("image_close_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = attachment.fileName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = attachment.fileSizeFormatted,
                                color = Color(0xFF94A3B8),
                                style = MaterialTheme.typography.labelSmall
                            )
                            if (scale > 1.05f) {
                                Text(
                                    text = "• Zoom: ${(scale * 100).toInt()}%",
                                    color = Color(0xFF38BDF8),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    // Zoom Out
                    IconButton(
                        onClick = {
                            scale = (scale - 0.5f).coerceAtLeast(1.0f)
                            if (scale <= 1.0f) offset = Offset.Zero
                        },
                        enabled = scale > 1.0f,
                        modifier = Modifier.testTag("image_zoom_out_button")
                    ) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = Color.White)
                    }

                    // Zoom In
                    IconButton(
                        onClick = { scale = (scale + 0.5f).coerceAtMost(5.0f) },
                        enabled = scale < 5.0f,
                        modifier = Modifier.testTag("image_zoom_in_button")
                    ) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = Color.White)
                    }

                    // Reset Zoom
                    if (scale > 1.05f || offset != Offset.Zero) {
                        IconButton(
                            onClick = {
                                scale = 1f
                                offset = Offset.Zero
                            },
                            modifier = Modifier.testTag("image_zoom_reset_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset Zoom", tint = Color(0xFF38BDF8))
                        }
                    }

                    // External Viewer
                    IconButton(
                        onClick = { StudyLibraryStorageManager.openAttachmentExternally(context, attachment) },
                        modifier = Modifier.testTag("image_open_external_button")
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = "Open in External App", tint = Color.White)
                    }

                    // Share/Export
                    IconButton(
                        onClick = { StudyLibraryStorageManager.exportAttachment(context, attachment) },
                        modifier = Modifier.testTag("image_share_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share Image", tint = Color.White)
                    }
                }
            }

            // Interactive Image Canvas
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1.2f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1.0f, 5.0f)
                            if (scale > 1.05f) {
                                offset = Offset(
                                    x = offset.x + pan.x,
                                    y = offset.y + pan.y
                                )
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLocalAvailable) {
                    val bitmap = remember(localFile.absolutePath) {
                        try {
                            BitmapFactory.decodeFile(localFile.absolutePath)
                        } catch (_: Exception) { null }
                    }

                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = attachment.fileName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offset.x
                                    translationY = offset.y
                                }
                                .testTag("image_view_canvas")
                        )
                    } else {
                        // Fallback to Coil
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(localFile)
                                .crossfade(true)
                                .build(),
                            contentDescription = attachment.fileName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offset.x
                                    translationY = offset.y
                                }
                                .testTag("image_view_canvas")
                        )
                    }
                } else if (attachment.remoteUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(attachment.remoteUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = attachment.fileName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            }
                            .testTag("image_view_canvas")
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Image file unavailable", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            // Bottom Info Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF0F172A),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Double-tap or pinch to inspect fine diagrams & formulas",
                        color = Color(0xFF94A3B8),
                        style = MaterialTheme.typography.labelSmall
                    )

                    Surface(
                        color = if (attachment.storageType == "FIREBASE") Color(0xFF065F46) else Color(0xFF1E293B),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (attachment.storageType == "FIREBASE") "Cloud Synced ☁️" else "Local Storage 📱",
                            color = if (attachment.storageType == "FIREBASE") Color(0xFF6EE7B7) else Color(0xFF94A3B8),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
