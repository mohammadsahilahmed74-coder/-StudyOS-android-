package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.library.StudyLibraryStorageManager
import com.example.data.model.NoteAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PdfViewerModal(
    attachment: NoteAttachment,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pageCount by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Zoom and pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Rendered page bitmaps cache
    val pageBitmaps = remember { mutableStateMapOf<Int, Bitmap>() }
    val listState = rememberLazyListState()

    // Calculate currently visible page for header indicator
    val currentVisiblePage by remember {
        derivedStateOf {
            if (pageCount == 0) 1
            else (listState.firstVisibleItemIndex + 1).coerceIn(1, pageCount)
        }
    }

    DisposableEffect(attachment.localPath) {
        val file = File(attachment.localPath)
        if (!file.exists()) {
            errorMessage = "File not found locally: ${attachment.fileName}"
            isLoading = false
            return@DisposableEffect onDispose {}
        }

        var parcelDescriptor: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null

        try {
            parcelDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(parcelDescriptor)
            pageCount = renderer.pageCount
            isLoading = false
        } catch (e: Exception) {
            Log.e("PdfViewerModal", "Failed to open PDF: ${e.message}", e)
            errorMessage = "Unable to render PDF (${e.localizedMessage ?: "corrupted or unsupported format"})"
            isLoading = false
        }

        onDispose {
            try {
                renderer?.close()
                parcelDescriptor?.close()
                pageBitmaps.values.forEach { if (!it.isRecycled) it.recycle() }
                pageBitmaps.clear()
            } catch (_: Exception) {}
        }
    }

    // Background page renderer
    LaunchedEffect(attachment.localPath, pageCount) {
        if (pageCount <= 0) return@LaunchedEffect
        val file = File(attachment.localPath)
        withContext(Dispatchers.IO) {
            try {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { pdfRenderer ->
                        for (pageIndex in 0 until pdfRenderer.pageCount) {
                            if (!pageBitmaps.containsKey(pageIndex)) {
                                val page = pdfRenderer.openPage(pageIndex)
                                // High-quality rendering: 2x scale for crisp reading on mobile displays
                                val renderWidth = (page.width * 2).coerceAtMost(2048)
                                val renderHeight = (page.height * 2).coerceAtMost(2800)
                                val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
                                val canvas = Canvas(bitmap)
                                canvas.drawColor(AndroidColor.WHITE)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                page.close()
                                pageBitmaps[pageIndex] = bitmap
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("PdfViewerModal", "Error rendering pages: ${e.message}")
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Rich Slate Dark
            .statusBarsPadding()
            .testTag("pdf_viewer_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF1E293B),
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
                        modifier = Modifier.testTag("pdf_close_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
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
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (pageCount > 0) {
                                Surface(
                                    color = Color(0xFF334155),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Page $currentVisiblePage of $pageCount",
                                        color = Color(0xFF94A3B8),
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (scale > 1.05f) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "${(scale * 100).toInt()}%",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Zoom Out
                    IconButton(
                        onClick = {
                            scale = (scale - 0.25f).coerceAtLeast(0.75f)
                            if (scale <= 1.0f) offset = Offset.Zero
                        },
                        enabled = scale > 0.75f,
                        modifier = Modifier.testTag("pdf_zoom_out_button")
                    ) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = Color.White)
                    }

                    // Zoom In
                    IconButton(
                        onClick = { scale = (scale + 0.25f).coerceAtMost(3.5f) },
                        enabled = scale < 3.5f,
                        modifier = Modifier.testTag("pdf_zoom_in_button")
                    ) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = Color.White)
                    }

                    // Reset Zoom
                    if (scale != 1f || offset != Offset.Zero) {
                        IconButton(
                            onClick = {
                                scale = 1f
                                offset = Offset.Zero
                            },
                            modifier = Modifier.testTag("pdf_zoom_reset_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset Zoom", tint = Color(0xFF38BDF8))
                        }
                    }

                    // External Viewer
                    IconButton(
                        onClick = { StudyLibraryStorageManager.openAttachmentExternally(context, attachment) },
                        modifier = Modifier.testTag("pdf_open_external_button")
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = "Open in External App", tint = Color.White)
                    }

                    // Share/Export
                    IconButton(
                        onClick = { StudyLibraryStorageManager.exportAttachment(context, attachment) },
                        modifier = Modifier.testTag("pdf_share_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share PDF", tint = Color.White)
                    }
                }
            }

            // PDF Content Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.75f, 4.0f)
                            if (scale > 1.05f) {
                                offset = Offset(
                                    x = offset.x + pan.x,
                                    y = offset.y + pan.y
                                )
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    }
            ) {
                if (isLoading) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading study document...", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    }
                } else if (errorMessage != null) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = errorMessage ?: "Error reading PDF",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You can still open and read this file using your device's external PDF reader.",
                            color = Color(0xFF94A3B8),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { StudyLibraryStorageManager.openAttachmentExternally(context, attachment) },
                            modifier = Modifier.testTag("pdf_error_open_external")
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open in External Reader")
                        }
                    }
                } else {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val viewportWidth = maxWidth

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offset.x
                                    translationY = offset.y
                                }
                                .testTag("pdf_pages_list"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            items(pageCount) { pageIndex ->
                                val bitmap = pageBitmaps[pageIndex]

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    shadowElevation = 6.dp
                                ) {
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "Page ${pageIndex + 1} of ${attachment.fileName}",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("pdf_page_${pageIndex + 1}")
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(480.dp)
                                                .background(Color(0xFF1E293B)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                CircularProgressIndicator(
                                                    color = Color(0xFF38BDF8),
                                                    modifier = Modifier.size(32.dp)
                                                )
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Text(
                                                    "Rendering page ${pageIndex + 1}...",
                                                    color = Color(0xFF94A3B8),
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        }
                                    }
                                }

                                // Small page number pill below each page
                                Text(
                                    text = "- Page ${pageIndex + 1} -",
                                    color = Color(0xFF64748B),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
