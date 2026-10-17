package com.example.storyworthy.ui

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.storyworthy.Entry
import com.example.storyworthy.ui.theme.StoryworthyTheme
import java.io.File

@Composable
fun EntryDetailScreen(
    entries: List<Entry>,
    initialIndex: Int = 0,
    onBackClick: () -> Unit = {},
    onDeleteEntry: (Entry) -> Unit = {},
    onToggleStar: (Entry) -> Unit = {}
) {
    var localEntries by remember(entries) { mutableStateOf(entries) }

    val clickedEntry = if (localEntries.isNotEmpty() && initialIndex in localEntries.indices) localEntries[initialIndex] else null

    // Sort entries chronologically (oldest to newest)
    val sortedEntries = remember(localEntries) {
        localEntries.sortedWith(compareBy<Entry> { it.date }.thenBy { it.time })
    }

    val targetIndex = if (clickedEntry != null) {
        sortedEntries.indexOf(clickedEntry).coerceAtLeast(0)
    } else {
        initialIndex.coerceIn(0, (sortedEntries.size - 1).coerceAtLeast(0))
    }

    val pageCount = sortedEntries.size.coerceAtLeast(1)
    val pagerState = rememberPagerState(
        initialPage = targetIndex.coerceIn(0, pageCount - 1),
        pageCount = { sortedEntries.size }
    )

    var showImageDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val currentEntry = if (sortedEntries.isNotEmpty() && pagerState.currentPage in sortedEntries.indices) {
        sortedEntries[pagerState.currentPage]
    } else null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Top Navigation Header
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = if (sortedEntries.isNotEmpty()) "Entry ${pagerState.currentPage + 1} of ${sortedEntries.size}" else "Entry",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.align(Alignment.Center)
                )

                if (currentEntry != null) {
                    var isStarredState by remember(currentEntry.id, currentEntry.isStarred) {
                        mutableStateOf(currentEntry.isStarred)
                    }
                    IconButton(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(36.dp),
                        onClick = {
                            isStarredState = !isStarredState
                            onToggleStar(currentEntry)
                        }
                    ) {
                        Icon(
                            imageVector = if (isStarredState) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = "Favorite entry",
                            tint = if (isStarredState) Color(0xFFFFD700) else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tags Row (Below header but ABOVE the "page")
            if (currentEntry != null && !currentEntry.tags.isNullOrEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    currentEntry.tags?.forEach { tag ->
                        AssistChip(
                            onClick = { },
                            label = { Text("#$tag", fontSize = 12.sp, fontWeight = FontWeight.Medium) }
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // The "Page" (HorizontalPager)
            if (sortedEntries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No entries found.")
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { page ->
                    val entry = sortedEntries[page]
                    SingleEntryPage(entry = entry)
                }
            }
        }

        // FABs in the Bottom Right Corner
        if (currentEntry != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Top FAB: Open attached images
                FloatingActionButton(
                    onClick = { showImageDialog = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Open Attached Images"
                    )
                }

                // Bottom FAB: Delete entry
                FloatingActionButton(
                    onClick = { showDeleteDialog = true },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Entry"
                    )
                }
            }
        }

        // Dialog for attached images
        if (showImageDialog && currentEntry != null) {
            val context = LocalContext.current
            AlertDialog(
                onDismissRequest = { showImageDialog = false },
                title = {
                    Text(
                        text = "Attached Images (${currentEntry.imageUris?.size ?: 0})",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    val imageUris = currentEntry.imageUris
                    if (imageUris.isNullOrEmpty()) {
                        Text("No photos attached to this entry.")
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            imageUris.forEach { uriStr ->
                                val uri = Uri.parse(uriStr)
                                val bitmap = remember(uri) { loadBitmapFromUri(context, uri) }
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = "Attached Photo",
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showImageDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // Dialog for entry deletion confirmation
        if (showDeleteDialog && currentEntry != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Entry", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete this entry? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            onDeleteEntry(currentEntry)
                            val updated = localEntries.filter { it != currentEntry }
                            localEntries = updated
                            if (updated.isEmpty()) {
                                onBackClick()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun SingleEntryPage(entry: Entry) {
    val emoji = remember(entry.mood) { extractEmoji(entry.mood) }
    val moodLabel = remember(entry.mood) { extractMoodLabel(entry.mood) }

    val strokes = remember(entry.strokesJson) { DraftManager.deserializeStrokes(entry.strokesJson) }

    Card(
        modifier = Modifier
            .fillMaxSize()
            .shadow(6.dp, shape = RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PaperColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    // Draw Left Red Margin Line across the paper
                    val marginX = 44.dp.toPx()
                    drawLine(
                        color = MarginLineColor,
                        start = Offset(marginX, 0f),
                        end = Offset(marginX, size.height),
                        strokeWidth = 1.5.dp.toPx()
                    )

                    // Draw continuous ruling lines down the entire page height
                    val lineHeightPx = 32.dp.toPx()
                    val firstLineY = 124.dp.toPx()
                    var y = firstLineY
                    while (y < size.height) {
                        drawLine(
                            color = RulingLineColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                        y += lineHeightPx
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 16.dp, bottom = 24.dp, end = 16.dp)
            ) {
                // Date & Time Header (aligned with red margin padding start = 52.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 52.dp, end = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(content = {
                        Text(
                            text = entry.date ?: "",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = entry.time ?: "",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                    })

                    // Mood Header Row
                    Row(
                        modifier = Modifier.padding(start = 52.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "Mood: $moodLabel",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            fontWeight = FontWeight.SemiBold
                        )
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFFF4E0),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(emoji, fontSize = 20.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                // Journal Title (aligned with start = 52.dp)
                Text(
                    text = entry.title ?: "",
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 52.dp, end = 8.dp, bottom = 8.dp)
                )

                // Audio Playback Area (if audio exists)
                if (!entry.audioPath.isNullOrBlank() && File(entry.audioPath).exists()) {
                    var audioPlaying by remember { mutableStateOf(false) }
                    var player by remember { mutableStateOf<android.media.MediaPlayer?>(null) }

                    DisposableEffect(entry.audioPath) {
                        onDispose {
                            player?.release()
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .padding(start = 52.dp, end = 16.dp, bottom = 8.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (audioPlaying) {
                                        player?.pause()
                                        audioPlaying = false
                                    } else {
                                        try {
                                            if (player == null) {
                                                player = android.media.MediaPlayer().apply {
                                                    setDataSource(entry.audioPath)
                                                    prepare()
                                                    setOnCompletionListener { audioPlaying = false }
                                                }
                                            }
                                            player?.start()
                                            audioPlaying = true
                                        } catch (_: Exception) {}
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (audioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play Audio",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = if (audioPlaying) "Playing Voice Note..." else "Voice Note Attached",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Entry Text Area & Drawing Canvas Overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    // Text
                    Text(
                        text = entry.text ?: "",
                        style = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 32.sp,
                            color = Color(0xFF2B2D42),
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.None
                            )
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 52.dp, end = 16.dp, top = 4.dp, bottom = 24.dp)
                    )

                    // Drawing Strokes Canvas Overlay
                    if (strokes.isNotEmpty()) {
                        Canvas(
                            modifier = Modifier
                                .matchParentSize()
                                .padding(start = 52.dp, end = 8.dp)
                        ) {
                            for ((path, color, strokeWidth) in strokes) {
                                drawPath(
                                    path = path,
                                    color = color,
                                    style = Stroke(
                                        width = strokeWidth,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun EntryDetailScreenPreview() {
    StoryworthyTheme {
        EntryDetailScreen(
            entries = listOf(
                Entry().apply {
                    title = "Sample Entry"
                    text = "This is a test entry with tags."
                    tags = listOf("journal", "reflect")
                }
            )
        )
    }
}
