package com.example.storyworthy.ui

import android.content.Context
import com.example.storyworthy.Entry
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import androidx.core.content.edit

// Paper and Line Colors
val PaperColor = Color(0xFFFAF8F3)
val RulingLineColor = Color(0xFFDCD6CD)
val MarginLineColor = Color(0xFFF2A49B)

data class DrawingStroke(
    val path: Path,
    val color: Color,
    val strokeWidth: Float,
    val points: List<Pair<Float, Float>> = emptyList()
)

data class DrawingSnapshot(
    val strokes: List<DrawingStroke>,
    val isEraser: Boolean,
    val color: Color,
    val strokeWidth: Float
)

data class SavedDraft(
    val title: String,
    val text: String,
    val audioPath: String?,
    val strokesJson: String,
    val imageUrisJson: String = "[]",
    val tagsJson: String = "[]"
)

object DraftManager {
    private const val PREF_NAME = "entry_draft_prefs"
    private const val KEY_HAS_DRAFT = "has_draft"
    private const val KEY_TITLE = "draft_title"
    private const val KEY_TEXT = "draft_text"
    private const val KEY_AUDIO_PATH = "draft_audio_path"
    private const val KEY_STROKES_JSON = "draft_strokes_json"
    private const val KEY_IMAGE_URIS_JSON = "draft_image_uris_json"
    private const val KEY_TAGS_JSON = "draft_tags_json"

    fun hasDraft(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_HAS_DRAFT, false)
    }

    fun saveDraft(
        context: Context,
        title: String,
        text: String,
        audioPath: String?,
        strokes: List<DrawingStroke>,
        imageUris: List<Uri>,
        tags: List<String>
    ) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val strokesArray = JSONArray()
        for ((_, color, strokeWidth, points) in strokes) {
            val obj = JSONObject()
            obj.put("color", color.toArgb())
            obj.put("width", strokeWidth.toDouble())
            val pointsArray = JSONArray()
            for ((first, second) in points) {
                val pt = JSONObject()
                pt.put("x", first.toDouble())
                pt.put("y", second.toDouble())
                pointsArray.put(pt)
            }
            obj.put("points", pointsArray)
            strokesArray.put(obj)
        }

        val imageUrisArray = JSONArray()
        for (uri in imageUris) {
            imageUrisArray.put(uri.toString())
        }

        val tagsArray = JSONArray()
        for (tag in tags) {
            tagsArray.put(tag)
        }

        prefs.edit().apply {
            putBoolean(KEY_HAS_DRAFT, true)
            putString(KEY_TITLE, title)
            putString(KEY_TEXT, text)
            putString(KEY_AUDIO_PATH, audioPath)
            putString(KEY_STROKES_JSON, strokesArray.toString())
            putString(KEY_IMAGE_URIS_JSON, imageUrisArray.toString())
            putString(KEY_TAGS_JSON, tagsArray.toString())
            apply()
        }
    }

    fun loadDraft(context: Context): SavedDraft? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_HAS_DRAFT, false)) return null

        return SavedDraft(
            title = prefs.getString(KEY_TITLE, "") ?: "",
            text = prefs.getString(KEY_TEXT, "") ?: "",
            audioPath = prefs.getString(KEY_AUDIO_PATH, null),
            strokesJson = prefs.getString(KEY_STROKES_JSON, "[]") ?: "[]",
            imageUrisJson = prefs.getString(KEY_IMAGE_URIS_JSON, "[]") ?: "[]",
            tagsJson = prefs.getString(KEY_TAGS_JSON, "[]") ?: "[]"
        )
    }

    fun clearDraft(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit { clear() }
    }

    fun serializeStrokes(strokes: List<DrawingStroke>): String {
        val strokesArray = JSONArray()
        for ((_, color, strokeWidth, points) in strokes) {
            val obj = JSONObject()
            obj.put("color", color.toArgb())
            obj.put("width", strokeWidth.toDouble())
            val pointsArray = JSONArray()
            for ((first, second) in points) {
                val pt = JSONObject()
                pt.put("x", first.toDouble())
                pt.put("y", second.toDouble())
                pointsArray.put(pt)
            }
            obj.put("points", pointsArray)
            strokesArray.put(obj)
        }
        return strokesArray.toString()
    }

    fun deserializeStrokes(jsonString: String): List<DrawingStroke> {
        val result = mutableListOf<DrawingStroke>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val color = Color(obj.getInt("color"))
                val width = obj.getDouble("width").toFloat()
                val pointsArray = obj.getJSONArray("points")
                val pointsList = mutableListOf<Pair<Float, Float>>()
                val path = Path()

                for (j in 0 until pointsArray.length()) {
                    val pt = pointsArray.getJSONObject(j)
                    val x = pt.getDouble("x").toFloat()
                    val y = pt.getDouble("y").toFloat()
                    pointsList.add(x to y)
                    if (j == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                result.add(DrawingStroke(path, color, width, pointsList))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    fun deserializeStringList(jsonString: String): List<String> {
        val result = mutableListOf<String>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                result.add(array.getString(i))
            }
        } catch (_: Exception) {}
        return result
    }
}

@Composable
fun AddMediaTagDialog(
    selectedImageUris: List<Uri>,
    tags: List<String>,
    onAddImageUris: (List<Uri>) -> Unit,
    onRemoveImageUri: (Uri) -> Unit,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var newTagText by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onAddImageUris(uris)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Photos & Tags", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Photos Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Photos", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose Photos from Gallery")
                    }

                    if (selectedImageUris.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(selectedImageUris) { uri ->
                                val bitmap = remember(uri) { loadBitmapFromUri(context, uri) }
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.LightGray)
                                ) {
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap,
                                            contentDescription = "Selected Photo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    IconButton(
                                        onClick = { onRemoveImageUri(uri) },
                                        modifier = Modifier
                                            .size(22.dp)
                                            .align(Alignment.TopEnd)
                                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove Photo",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Tags Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tag(s)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newTagText,
                            onValueChange = { newTagText = it },
                            placeholder = { Text("e.g. Travel, Family...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Button(
                            onClick = {
                                if (newTagText.isNotBlank()) {
                                    onAddTag(newTagText.trim())
                                    newTagText = ""
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Add")
                        }
                    }

                    if (tags.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            tags.forEach { tag ->
                                InputChip(
                                    selected = true,
                                    onClick = { onRemoveTag(tag) },
                                    label = { Text("#$tag") },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove Tag",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

fun extractEmoji(mood: String): String {
    if (mood.isBlank()) return "😊"
    val trimmed = mood.trim()
    val parts = trimmed.split(Regex("\\s+"), limit = 2)
    return parts.getOrNull(0) ?: "😊"
}

fun extractMoodLabel(mood: String): String {
    if (mood.isBlank()) return "Content"
    val trimmed = mood.trim()
    val parts = trimmed.split(Regex("\\s+"), limit = 2)
    return parts.getOrNull(1) ?: trimmed
}

fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
}

fun loadBitmapFromUri(context: Context, uri: Uri): ImageBitmap? {
    return try {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source).asImageBitmap()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Selects an entry title from database entries at random, prioritizing recent entries
 * or entries with similar moods.
 */
fun getRandomContextTitle(entries: List<Entry>, selectedMood: String): String {
    if (entries.isEmpty()) return ""

    val cleanTargetMood = selectedMood.lowercase()
    // Find entries with similar/matching mood
    val matchingMoodEntries = entries.filter { entry ->
        val entryMood = (entry.mood ?: "").lowercase()
        entryMood.isNotBlank() && (entryMood.contains(cleanTargetMood) || cleanTargetMood.contains(entryMood))
    }

    val candidates = if (matchingMoodEntries.isNotEmpty()) {
        matchingMoodEntries.takeLast(5)
    } else {
        entries.takeLast(5)
    }

    val chosen = candidates.randomOrNull() ?: entries.randomOrNull()
    return chosen?.title?.ifBlank { "Untitled Entry" } ?: ""
}
