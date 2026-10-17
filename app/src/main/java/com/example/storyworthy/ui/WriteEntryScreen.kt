package com.example.storyworthy.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.storyworthy.Entry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.example.storyworthy.ui.theme.StoryworthyTheme
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.time.Duration.Companion.milliseconds

enum class EditMode {
    TEXT,
    DRAWING,
    VOICE
}

enum class TextHeading {
    NORMAL,
    H1,
    H2
}

data class RichSpan(
    val start: Int,
    val end: Int,
    val style: SpanStyle
)


@Stable
class AudioRecorderState(val context: Context) {
    var isRecording by mutableStateOf(false)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var audioFile by mutableStateOf<File?>(null)
        private set
    var recordingDuration by mutableIntStateOf(0)
    var currentPlaybackPosition by mutableIntStateOf(0)
        private set
    var totalDuration by mutableIntStateOf(0)
        private set

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null

    fun startRecording() {
        try {
            stopPlayback()
            val outputFile = File(context.cacheDir, "entry_audio_${System.currentTimeMillis()}.m4a")
            val recorder = MediaRecorder(context).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            audioFile = outputFile
            isRecording = true
            recordingDuration = 0
        } catch (e: Exception) {
            e.printStackTrace()
            stopRecording()
        }
    }

    fun stopRecording() {
        if (!isRecording) return
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
            isRecording = false
            audioFile?.let { file ->
                try {
                    val mp = MediaPlayer().apply { setDataSource(file.absolutePath); prepare() }
                    totalDuration = mp.duration / 1000
                    mp.release()
                } catch (_: Exception) {}
            }
        }
    }

    fun startPlayback() {
        val file = audioFile ?: return
        try {
            stopPlayback()
            val player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    this@AudioRecorderState.isPlaying = false
                    this@AudioRecorderState.currentPlaybackPosition = 0
                }
                start()
            }
            mediaPlayer = player
            totalDuration = player.duration / 1000
            isPlaying = true
        } catch (e: Exception) {
            e.printStackTrace()
            stopPlayback()
        }
    }

    fun pausePlayback() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    isPlaying = false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer = null
            isPlaying = false
            currentPlaybackPosition = 0
        }
    }

    fun updatePlaybackProgress() {
        mediaPlayer?.let { mp ->
            if (isPlaying) {
                currentPlaybackPosition = mp.currentPosition / 1000
            }
        }
    }

    fun deleteAudio() {
        stopPlayback()
        stopRecording()
        audioFile?.delete()
        audioFile = null
        recordingDuration = 0
        currentPlaybackPosition = 0
        totalDuration = 0
    }

    fun loadExistingAudioFile(file: File) {
        if (file.exists()) {
            audioFile = file
            try {
                val mp = MediaPlayer().apply { setDataSource(file.absolutePath); prepare() }
                totalDuration = mp.duration / 1000
                mp.release()
            } catch (_: Exception) {}
        }
    }

    fun release() {
        stopRecording()
        stopPlayback()
    }
}



@Composable
fun AudioRecordSection(
    audioState: AudioRecorderState,
    onStartRecordRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when {
                audioState.isRecording -> {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                    )
                    Text(
                        text = "Recording: ${formatDuration(audioState.recordingDuration)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { audioState.stopRecording() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop Recording",
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                audioState.audioFile != null -> {
                    IconButton(
                        onClick = {
                            if (audioState.isPlaying) {
                                audioState.pausePlayback()
                            } else {
                                audioState.startPlayback()
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (audioState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (audioState.isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = if (audioState.isPlaying) {
                            "${formatDuration(audioState.currentPlaybackPosition)} / ${formatDuration(audioState.totalDuration)}"
                        } else {
                            "Voice Note (${formatDuration(audioState.totalDuration)})"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { audioState.deleteAudio() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Audio",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                else -> {
                    IconButton(
                        onClick = onStartRecordRequest,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Record Voice Note",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Add Voice Note",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { onStartRecordRequest() }
                    )
                }
            }
        }
    }
}

// Color Palettes
val TextColorPalette = listOf(
    Color(0xFF2B2D42), // Charcoal / Default
    Color(0xFF1D3557), // Navy
    Color(0xFFC1121F), // Crimson
    Color(0xFF15803D), // Forest Green
    Color(0xFF7E22CE), // Purple
    Color(0xFF78350F)  // Brown
)

val HighlightColorPalette = listOf(
    Color.Transparent, // Clear
    Color(0xFFFFF3BF), // Sunlight Yellow
    Color(0xFFD3F9D8), // Mint Green
    Color(0xFFFFD8A8), // Pastel Blossom
    Color(0xFFE7F5FF), // Soft Blue
    Color(0xFFF3D9FA)  // Lavender
)

val DrawColorPalette = listOf(
    Color(0xFF2B2D42), // Dark Slate
    Color(0xFF1D3557), // Navy Blue
    Color(0xFFC1121F), // Red Ink
    Color(0xFF15803D), // Green Ink
    Color(0xFF7E22CE), // Purple
    Color(0xFF78350F), // Mocha
    Color(0xFFEAB308)  // Gold
)

@Composable
fun WriteEntryScreen(
    modifier: Modifier = Modifier,
    mood: String = "",
    initialContent: String = "",
    onBackClick: () -> Unit = {},
    onSubmit: () -> Unit = {}
) {
    var title by remember { mutableStateOf("") }
    var activeMode by remember { mutableStateOf(EditMode.TEXT) }

    // Rich Text State
    var rawText by remember(initialContent) { mutableStateOf(initialContent) }
    var spans by remember { mutableStateOf(listOf<RichSpan>()) }
    var textFieldValue by remember(initialContent) {
        val initialAnnotated = createAnnotatedString(initialContent, emptyList())
        mutableStateOf(TextFieldValue(annotatedString = initialAnnotated, selection = TextRange(initialContent.length)))
    }

    // Active Text Formatting Toggles
    var isBoldActive by remember { mutableStateOf(false) }
    var isItalicActive by remember { mutableStateOf(false) }
    var isUnderlineActive by remember { mutableStateOf(false) }
    var selectedTextColor by remember { mutableStateOf(TextColorPalette.first()) }
    var selectedHighlightColor by remember { mutableStateOf(Color.Transparent) }
    var activeHeading by remember { mutableStateOf(TextHeading.NORMAL) }

    // Drawing Canvas State
    val strokes = remember { mutableStateListOf<DrawingStroke>() }
    var isEraser by remember { mutableStateOf(false) }
    var selectedDrawColor by remember { mutableStateOf(DrawColorPalette.first()) }
    var selectedStrokeWidth by remember { mutableFloatStateOf(6f) }
    val undoHistory = remember { mutableStateListOf<DrawingSnapshot>() }

    fun saveSnapshot() {
        undoHistory.add(
            DrawingSnapshot(
                strokes = strokes.toList(),
                isEraser = isEraser,
                color = selectedDrawColor,
                strokeWidth = selectedStrokeWidth
            )
        )
    }

    fun undo() {
        if (undoHistory.isNotEmpty()) {
            val lastState = undoHistory.removeAt(undoHistory.lastIndex)
            strokes.clear()
            strokes.addAll(lastState.strokes)
            isEraser = lastState.isEraser
            selectedDrawColor = lastState.color
            selectedStrokeWidth = lastState.strokeWidth
        }
    }

    var currentPath by remember { mutableStateOf<Path?>(null) }
    var currentPoints by remember { mutableStateOf(listOf<Pair<Float, Float>>()) }
    var currentPathTrigger by remember { mutableIntStateOf(0) }

    // Audio Recorder State
    val context = LocalContext.current
    val audioRecorderState = remember(context) { AudioRecorderState(context) }

    DisposableEffect(Unit) {
        onDispose {
            audioRecorderState.release()
        }
    }

    LaunchedEffect(audioRecorderState.isRecording) {
        if (audioRecorderState.isRecording) {
            while (isActive) {
                delay(1000.milliseconds)
                audioRecorderState.recordingDuration++
            }
        }
    }

    LaunchedEffect(audioRecorderState.isPlaying) {
        if (audioRecorderState.isPlaying) {
            while (isActive) {
                delay(300.milliseconds)
                audioRecorderState.updatePlaybackProgress()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            audioRecorderState.startRecording()
        }
    }

    fun startAudioRecording() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            audioRecorderState.startRecording()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Photos & Tags State
    var showMediaTagDialog by remember { mutableStateOf(false) }
    var selectedImageUris by remember { mutableStateOf(listOf<Uri>()) }
    var tags by remember { mutableStateOf(listOf<String>()) }

    // Draft Management State
    var showResumeDraftDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (DraftManager.hasDraft(context)) {
            showResumeDraftDialog = true
        }
    }

    fun performSaveDraft() {
        DraftManager.saveDraft(
            context = context,
            title = title,
            text = rawText,
            audioPath = audioRecorderState.audioFile?.absolutePath,
            strokes = strokes,
            imageUris = selectedImageUris,
            tags = tags
        )
        Toast.makeText(context, "Draft saved!", Toast.LENGTH_SHORT).show()
    }

    // Firebase Upload & Error State
    var isUploading by remember { mutableStateOf(false) }
    var uploadErrorReason by remember { mutableStateOf<String?>(null) }
    var uploadErrorSuggestion by remember { mutableStateOf<String?>(null) }
    var showUploadErrorDialog by remember { mutableStateOf(false) }

    val isInspectionMode = LocalInspectionMode.current
    val firestore = remember { if (!isInspectionMode) try { FirebaseFirestore.getInstance() } catch (_: Exception) { null } else null }
    val auth = remember { if (!isInspectionMode) try { FirebaseAuth.getInstance() } catch (_: Exception) { null } else null }
    val coroutineScope = rememberCoroutineScope()

    fun saveAndUploadEntry() {
        if (auth == null || firestore == null) {
            uploadErrorReason = "Preview Mode"
            uploadErrorSuggestion = "Firebase is not available in preview mode."
            showUploadErrorDialog = true
            return
        }
        val currentUser = auth.currentUser
        if (currentUser == null) {
            uploadErrorReason = "Authentication Error"
            uploadErrorSuggestion = "You are currently not signed in. Please sign in to your Storyworthy account and try again."
            showUploadErrorDialog = true
            return
        }
        if (title.isBlank()) {
            Toast.makeText(context, "Please enter a title for your entry.", Toast.LENGTH_SHORT).show()
            return
        }

        isUploading = true
        val entryDate = LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
        val entryTime = LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.US))

        val newEntry = Entry(
            title,
            rawText,
            entryDate,
            entryTime,
            mood,
            tags,
            selectedImageUris.map { it.toString() },
            DraftManager.serializeStrokes(strokes),
            audioRecorderState.audioFile?.absolutePath ?: ""
        )

        coroutineScope.launch {
            try {
                withTimeout(20.seconds) { // 20 seconds timeout
                    firestore.collection("users")
                        .document(currentUser.uid)
                        .collection("user-entries")
                        .add(newEntry)
                        .await()
                }
                isUploading = false
                DraftManager.clearDraft(context)
                onSubmit()
            } catch (_: TimeoutCancellationException) {
                isUploading = false
                uploadErrorReason = "Upload Timeout"
                uploadErrorSuggestion = "The entry upload timed out after 20 seconds. Please check your internet connection and try again, or save your work as a draft."
                showUploadErrorDialog = true
            } catch (exception: Exception) {
                isUploading = false
                val errorMsg = exception.message ?: "Unknown Firebase Error"
                val errorCode = (exception as? FirebaseFirestoreException)?.code?.name ?: "UNKNOWN"

                uploadErrorReason = "Firebase Error [$errorCode]: $errorMsg"
                uploadErrorSuggestion = when {
                    errorCode == "NOT_FOUND" || errorMsg.contains("does not exist", ignoreCase = true) || errorMsg.contains("database", ignoreCase = true) ->
                        "The Cloud Firestore database has not been created in Firebase Console for project 'storyworthy-6af5c'. Visit https://console.cloud.google.com/datastore/setup?project=storyworthy-6af5c to initialize Cloud Firestore."
                    errorCode == "PERMISSION_DENIED" || errorMsg.contains("permission", ignoreCase = true) ->
                        "Your account lacks permission to write to Firebase Firestore. Verify database rules or sign in again."
                    errorCode == "UNAVAILABLE" || errorMsg.contains("network", ignoreCase = true) ->
                        "Network connection issue. Check your Wi-Fi or mobile data connection and try again."
                    else ->
                        "An error occurred while uploading your entry to the server. Check your connection or save as a draft."
                }
                showUploadErrorDialog = true
            }
        }
    }

    // Discard Confirmation Dialog State & System Back Handler
    var showDiscardDialog by remember { mutableStateOf(false) }

    val hasUnsavedContent = remember(title, rawText, initialContent, strokes.size, audioRecorderState.audioFile, selectedImageUris.size, tags.size) {
        title.isNotBlank() || (rawText != initialContent && rawText.isNotBlank()) || strokes.isNotEmpty() || audioRecorderState.audioFile != null || selectedImageUris.isNotEmpty() || tags.isNotEmpty()
    }

    fun handleBackClick() {
        if (hasUnsavedContent) {
            showDiscardDialog = true
        } else {
            onBackClick()
        }
    }

    BackHandler(enabled = hasUnsavedContent) {
        showDiscardDialog = true
    }

    // Synchronize TextFieldValue changes with RichSpans
    fun onTextFieldValueChange(newValue: TextFieldValue) {
        val oldText = rawText
        val newText = newValue.text

        if (oldText != newText) {
            rawText = newText
            // Adjust existing span ranges to account for inserted or deleted characters
            spans = adjustSpans(oldText, newText, spans, textFieldValue.selection)

            // If active formatting styles are enabled while typing new characters, apply them
            val addedLength = newText.length - oldText.length
            if (addedLength > 0) {
                val insertStart = newValue.selection.min - addedLength
                val insertEnd = newValue.selection.min
                if (insertStart in 0..<insertEnd) {
                    val activeStyle = SpanStyle(
                        fontWeight = if (isBoldActive || activeHeading != TextHeading.NORMAL) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (isItalicActive) FontStyle.Italic else FontStyle.Normal,
                        textDecoration = if (isUnderlineActive) TextDecoration.Underline else TextDecoration.None,
                        color = selectedTextColor,
                        background = selectedHighlightColor,
                        fontSize = when (activeHeading) {
                            TextHeading.H1 -> 24.sp
                            TextHeading.H2 -> 20.sp
                            TextHeading.NORMAL -> 16.sp
                        }
                    )
                    spans = spans + RichSpan(insertStart, insertEnd, activeStyle)
                }
            }
        }

        val annotated = createAnnotatedString(newText, spans)
        textFieldValue = newValue.copy(annotatedString = annotated)
    }

    // Helper to apply formatting to selected range
    fun applyStyleToSelection(styleToApply: SpanStyle) {
        val selection = textFieldValue.selection
        if (!selection.collapsed) {
            val start = selection.min
            val end = selection.max
            spans = spans + RichSpan(start, end, styleToApply)
            val annotated = createAnnotatedString(rawText, spans)
            textFieldValue = textFieldValue.copy(annotatedString = annotated)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Top Navigation Header
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = ::handleBackClick,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = "New Entry",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.align(Alignment.Center)
                )

                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { showMediaTagDialog = true },
                        modifier = Modifier.align(Alignment.CenterEnd)

                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Add Image/Tag",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(50.dp)
                        )
                    }
                }
            }

            // Mode Selector Switcher (Write vs Draw vs Voice)
            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    selected = activeMode == EditMode.TEXT,
                    onClick = { activeMode = EditMode.TEXT },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                ) {
                    Text("Write", fontSize = 13.sp)
                }
                SegmentedButton(
                    selected = activeMode == EditMode.DRAWING,
                    onClick = { activeMode = EditMode.DRAWING },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Draw,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                ) {
                    Text("Draw", fontSize = 13.sp)
                }
                SegmentedButton(
                    selected = activeMode == EditMode.VOICE,
                    onClick = { activeMode = EditMode.VOICE },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                ) {
                    Text("Voice", fontSize = 13.sp)
                }
            }

        // Toolbars
        when (activeMode) {
            EditMode.TEXT -> {
                TextFormattingToolbar(
                    isBoldActive = isBoldActive,
                    onBoldToggle = {
                        isBoldActive = !isBoldActive
                        applyStyleToSelection(SpanStyle(fontWeight = if (isBoldActive) FontWeight.Bold else FontWeight.Normal))
                    },
                    isItalicActive = isItalicActive,
                    onItalicToggle = {
                        isItalicActive = !isItalicActive
                        applyStyleToSelection(SpanStyle(fontStyle = if (isItalicActive) FontStyle.Italic else FontStyle.Normal))
                    },
                    isUnderlineActive = isUnderlineActive,
                    onUnderlineToggle = {
                        isUnderlineActive = !isUnderlineActive
                        applyStyleToSelection(SpanStyle(textDecoration = if (isUnderlineActive) TextDecoration.Underline else TextDecoration.None))
                    },
                    activeHeading = activeHeading,
                    onHeadingSelect = { heading ->
                        activeHeading = heading
                        val size = when (heading) {
                            TextHeading.H1 -> 24.sp
                            TextHeading.H2 -> 20.sp
                            TextHeading.NORMAL -> 16.sp
                        }
                        val weight = if (heading != TextHeading.NORMAL) FontWeight.Bold else FontWeight.Normal
                        applyStyleToSelection(SpanStyle(fontSize = size, fontWeight = weight))
                    },
                    selectedTextColor = selectedTextColor,
                    onTextColorSelect = { color ->
                        selectedTextColor = color
                        applyStyleToSelection(SpanStyle(color = color))
                    },
                    selectedHighlightColor = selectedHighlightColor,
                    onHighlightColorSelect = { color ->
                        selectedHighlightColor = color
                        applyStyleToSelection(SpanStyle(background = color))
                    }
                )
            }
            EditMode.DRAWING -> {
                DrawingToolbar(
                    isEraser = isEraser,
                    onEraserToggle = { isEraser = it },
                    selectedColor = selectedDrawColor,
                    onColorSelect = {
                        selectedDrawColor = it
                        isEraser = false
                    },
                    selectedWidth = selectedStrokeWidth,
                    onWidthSelect = { selectedStrokeWidth = it },
                    onUndo = { undo() },
                    onClear = {
                        if (strokes.isNotEmpty()) {
                            saveSnapshot()
                            strokes.clear()
                        }
                    }
                )
            }
            EditMode.VOICE -> {
                AudioRecordSection(
                    audioState = audioRecorderState,
                    onStartRecordRequest = ::startAudioRecording,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Journal Page (Simulation of actual notebook paper with ruling lines and red margin)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .shadow(6.dp, shape = RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PaperColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        // Draw Left Red Margin Line across the full paper
                        val marginX = 44.dp.toPx()
                        drawLine(
                            color = MarginLineColor,
                            start = Offset(marginX, 0f),
                            end = Offset(marginX, size.height),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 12.dp, end = 16.dp)
                ) {
                    // Journal Page Date Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 52.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = "Mood: $mood",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                    }

                    // Journal Title Input
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Entry Title...", color = Color.Gray, fontWeight = FontWeight.SemiBold) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        textStyle = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 36.dp, bottom = 4.dp)
                    )

                    // Attached Tags & Photos Summary
                    if (tags.isNotEmpty() || selectedImageUris.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 52.dp, end = 16.dp, bottom = 6.dp)
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            tags.forEach { tag ->
                                AssistChip(
                                    onClick = { showMediaTagDialog = true },
                                    label = { Text("#$tag", fontSize = 12.sp) }
                                )
                            }
                            selectedImageUris.forEach { uri ->
                                val bitmap = remember(uri) { loadBitmapFromUri(context, uri) }
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = "Attached Photo",
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { showMediaTagDialog = true },
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }

                    // Entry Text Area with perfectly aligned notebook ruling lines
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .drawBehind {
                                val lineHeightPx = 32.dp.toPx()
                                val firstBaselineY = 28.dp.toPx()
                                var y = firstBaselineY
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
                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = { onTextFieldValueChange(it) },
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                lineHeight = 32.sp,
                                color = Color(0xFF2B2D42),
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                lineHeightStyle = LineHeightStyle(
                                    alignment = LineHeightStyle.Alignment.Center,
                                    trim = LineHeightStyle.Trim.None
                                )
                            ),
                            enabled = activeMode == EditMode.TEXT,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 52.dp, end = 8.dp, top = 0.dp)
                        )

                        // Drawing Canvas Layer (overlaying the page)
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (activeMode == EditMode.DRAWING) {
                                        Modifier.pointerInput(isEraser, selectedDrawColor, selectedStrokeWidth) {
                                            detectDragGestures(
                                                onDragStart = { offset ->
                                                    currentPoints = listOf(offset.x to offset.y)
                                                    val newPath = Path().apply { moveTo(offset.x, offset.y) }
                                                    currentPath = newPath
                                                    currentPathTrigger++
                                                },
                                                onDrag = { change, _ ->
                                                    currentPoints = currentPoints + (change.position.x to change.position.y)
                                                    currentPath?.lineTo(change.position.x, change.position.y)
                                                    currentPathTrigger++
                                                },
                                                onDragEnd = {
                                                    currentPath?.let { path ->
                                                        saveSnapshot()
                                                        strokes.add(
                                                            DrawingStroke(
                                                                path = path,
                                                                color = if (isEraser) PaperColor else selectedDrawColor,
                                                                strokeWidth = if (isEraser) selectedStrokeWidth * 3f else selectedStrokeWidth,
                                                                points = currentPoints
                                                            )
                                                        )
                                                    }
                                                    currentPath = null
                                                }
                                            )
                                        }
                                    } else Modifier
                                )
                        ) {
                            // Suppress unused trigger warning for recomposition
                            @Suppress("UNUSED_EXPRESSION") currentPathTrigger

                            // Draw completed strokes
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
                            // Draw in-progress active stroke
                            currentPath?.let { path ->
                                drawPath(
                                    path = path,
                                    color = if (isEraser) PaperColor else selectedDrawColor,
                                    style = Stroke(
                                        width = if (isEraser) selectedStrokeWidth * 3f else selectedStrokeWidth,
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

        // Discard / Save as Draft Dialog
        if (showDiscardDialog) {
            AlertDialog(
                onDismissRequest = { showDiscardDialog = false },
                title = { Text("Unsaved Changes") },
                text = { Text("You have unsaved changes in your entry. Would you like to save as a draft, discard changes, or keep editing?") },
                confirmButton = {
                    Button(
                        onClick = {
                            performSaveDraft()
                            showDiscardDialog = false
                            onBackClick()
                        }
                    ) {
                        Text("Save as Draft")
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = {
                                DraftManager.clearDraft(context)
                                showDiscardDialog = false
                                onBackClick()
                            }
                        ) {
                            Text("Discard", color = MaterialTheme.colorScheme.error)
                        }
                        TextButton(onClick = { showDiscardDialog = false }) {
                            Text("Keep Editing")
                        }
                    }
                }
            )
        }

        // Resume Saved Draft Dialog
        if (showResumeDraftDialog) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Draft Found") },
                text = { Text("A saved draft entry exists. Would you like to continue editing your draft or start fresh?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showResumeDraftDialog = false
                            DraftManager.loadDraft(context)?.let { draft ->
                                title = draft.title
                                rawText = draft.text
                                spans = emptyList()
                                textFieldValue = TextFieldValue(
                                    annotatedString = createAnnotatedString(draft.text, emptyList()),
                                    selection = TextRange(draft.text.length)
                                )
                                strokes.clear()
                                strokes.addAll(DraftManager.deserializeStrokes(draft.strokesJson))
                                draft.audioPath?.let { path ->
                                    audioRecorderState.loadExistingAudioFile(File(path))
                                }
                                selectedImageUris = DraftManager.deserializeStringList(draft.imageUrisJson).map { it.toUri() }
                                tags = DraftManager.deserializeStringList(draft.tagsJson)
                            }
                        }
                    ) {
                        Text("Continue Draft")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showResumeDraftDialog = false
                            DraftManager.clearDraft(context)
                        }
                    ) {
                        Text("Start Fresh", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }

        // Photos & Tags Selection Dialog
        if (showMediaTagDialog) {
            AddMediaTagDialog(
                selectedImageUris = selectedImageUris,
                tags = tags,
                onAddImageUris = { uris ->
                    selectedImageUris = (selectedImageUris + uris).distinct()
                },
                onRemoveImageUri = { uri ->
                    selectedImageUris = selectedImageUris - uri
                },
                onAddTag = { tag ->
                    if (!tags.contains(tag)) {
                        tags = tags + tag
                    }
                },
                onRemoveTag = { tag ->
                    tags = tags - tag
                },
                onDismiss = { showMediaTagDialog = false }
            )
        }

        // Upload Error Popup Dialog
        if (showUploadErrorDialog) {
            AlertDialog(
                onDismissRequest = { showUploadErrorDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = "Upload Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "Unable to Upload Entry",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "The entry could not be saved.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Reason:",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = uploadErrorReason ?: "Unknown error",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = uploadErrorSuggestion ?: "Check your connection and try again.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showUploadErrorDialog = false }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            performSaveDraft()
                            showUploadErrorDialog = false
                        }
                    ) {
                        Text("Save as Draft")
                    }
                }
            )
        }

        // Loading Overlay while uploading
        if (isUploading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Saving entry...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    // Bottom Floating Action Buttons (Draft & Save)
    Row(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExtendedFloatingActionButton(
            onClick = { performSaveDraft() },
            icon = {
                Icon(
                    imageVector = Icons.Default.BookmarkBorder,
                    contentDescription = "Save Draft",
                    modifier = Modifier.size(18.dp)
                )
            },
            text = { Text("Draft", fontSize = 14.sp, fontWeight = FontWeight.SemiBold) },
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
        )

        ExtendedFloatingActionButton(
            onClick = { saveAndUploadEntry() },
            icon = {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Save Entry",
                    modifier = Modifier.size(20.dp)
                )
            },
            text = { Text("Save", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
        )
    }
}
}

// Rich Text Formatting Toolbar Component
@Composable
fun TextFormattingToolbar(
    isBoldActive: Boolean,
    onBoldToggle: () -> Unit,
    isItalicActive: Boolean,
    onItalicToggle: () -> Unit,
    isUnderlineActive: Boolean,
    onUnderlineToggle: () -> Unit,
    activeHeading: TextHeading,
    onHeadingSelect: (TextHeading) -> Unit,
    selectedTextColor: Color,
    onTextColorSelect: (Color) -> Unit,
    selectedHighlightColor: Color,
    onHighlightColorSelect: (Color) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Bold
            FilterChip(
                selected = isBoldActive,
                onClick = onBoldToggle,
                label = { Text("B", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            )

            // Italic
            FilterChip(
                selected = isItalicActive,
                onClick = onItalicToggle,
                label = { Text("I", fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            )

            // Underline
            FilterChip(
                selected = isUnderlineActive,
                onClick = onUnderlineToggle,
                label = { Text("U", textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            )

            VerticalDivider(modifier = Modifier.height(24.dp))

            // Headings
            FilterChip(
                selected = activeHeading == TextHeading.H1,
                onClick = { onHeadingSelect(if (activeHeading == TextHeading.H1) TextHeading.NORMAL else TextHeading.H1) },
                label = { Text("H1", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )

            FilterChip(
                selected = activeHeading == TextHeading.H2,
                onClick = { onHeadingSelect(if (activeHeading == TextHeading.H2) TextHeading.NORMAL else TextHeading.H2) },
                label = { Text("H2", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )

            VerticalDivider(modifier = Modifier.height(24.dp))

            // Text Color Palette
            Text("Color: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextColorPalette.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (selectedTextColor == color) 2.dp else 0.dp,
                            color = if (selectedTextColor == color) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { onTextColorSelect(color) }
                )
            }

            VerticalDivider(modifier = Modifier.height(24.dp))

            // Highlight Color Palette
            Text("Highlight: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HighlightColorPalette.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (color == Color.Transparent) Color.White else color)
                        .border(
                            width = if (selectedHighlightColor == color) 2.dp else 1.dp,
                            color = if (selectedHighlightColor == color) MaterialTheme.colorScheme.primary else Color.LightGray,
                            shape = CircleShape
                        )
                        .clickable { onHighlightColorSelect(color) }
                ) {
                    if (color == Color.Transparent) {
                        Icon(
                            imageVector = Icons.Default.FormatColorReset,
                            contentDescription = "No Highlight",
                            tint = Color.Gray,
                            modifier = Modifier.size(12.dp).align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}

// Drawing Toolbar Component
@Composable
fun DrawingToolbar(
    isEraser: Boolean,
    onEraserToggle: (Boolean) -> Unit,
    selectedColor: Color,
    onColorSelect: (Color) -> Unit,
    selectedWidth: Float,
    onWidthSelect: (Float) -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Pen Tool
            FilterChip(
                selected = !isEraser,
                onClick = { onEraserToggle(false) },
                label = { Text("Pen") },
                leadingIcon = { Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )

            // Eraser Tool
            FilterChip(
                selected = isEraser,
                onClick = { onEraserToggle(true) },
                label = { Text("Eraser") },
                leadingIcon = { Icon(Icons.Default.AutoFixNormal, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )

            VerticalDivider(modifier = Modifier.height(24.dp))

            // Stroke Width Selector
            Text("Size:", style = MaterialTheme.typography.bodySmall)
            listOf(3f to "Thin", 7f to "Medium", 14f to "Thick").forEach { (width, label) ->
                FilterChip(
                    selected = selectedWidth == width,
                    onClick = { onWidthSelect(width) },
                    label = { Text(label, fontSize = 11.sp) }
                )
            }

            VerticalDivider(modifier = Modifier.height(24.dp))

            // Color Palette
            Text("Color:", style = MaterialTheme.typography.bodySmall)
            DrawColorPalette.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (selectedColor == color && !isEraser) 2.dp else 0.dp,
                            color = if (selectedColor == color && !isEraser) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { onColorSelect(color) }
                )
            }

            VerticalDivider(modifier = Modifier.height(24.dp))

            IconButton(onClick = onUndo, modifier = Modifier.size(32.dp)) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", modifier = Modifier.size(18.dp))
            }

            IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Clear Canvas", modifier = Modifier.size(18.dp))
            }
        }
    }
}

// Adjust span ranges when text changes (characters inserted or deleted)
fun adjustSpans(oldText: String, newText: String, spans: List<RichSpan>, selection: TextRange): List<RichSpan> {
    val delta = newText.length - oldText.length
    if (delta == 0) return spans
    val cursor = selection.min

    return spans.mapNotNull { span ->
        when {
            span.start >= cursor -> {
                val newStart = (span.start + delta).coerceAtLeast(cursor)
                val newEnd = (span.end + delta).coerceAtLeast(newStart)
                if (newStart < newEnd) span.copy(start = newStart, end = newEnd) else null
            }
            span.start < cursor && span.end >= cursor -> {
                val newEnd = (span.end + delta).coerceAtLeast(span.start)
                if (span.start < newEnd) span.copy(end = newEnd) else null
            }
            else -> span
        }
    }
}

// Build AnnotatedString from text and spans list
fun createAnnotatedString(text: String, spans: List<RichSpan>): AnnotatedString {
    val builder = AnnotatedString.Builder(text)
    for ((start, end, style) in spans) {
        val validStart = start.coerceIn(0, text.length)
        val validEnd = end.coerceIn(validStart, text.length)
        if (validStart < validEnd) {
            builder.addStyle(style, validStart, validEnd)
        }
    }
    return builder.toAnnotatedString()
}

@Preview(showBackground = true)
@Composable
fun WriteEntryScreenPreview() {
    StoryworthyTheme {
        WriteEntryScreen(
            initialContent = "Today was a wonderful day filled with learning and creativity."
        )
    }
}
