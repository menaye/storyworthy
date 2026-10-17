package com.example.storyworthy.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.storyworthy.ui.theme.StoryworthyTheme

data class MoodOption(
    val id: String,
    val emoji: String,
    val label: String
)

val defaultMoodOptions = listOf(
    MoodOption("awful", "😩", "Awful"),
    MoodOption("sad", "😔", "Sad"),
    MoodOption("neutral", "😐", "Neutral"),
    MoodOption("happy", "😊", "Happy"),
    MoodOption("content", "😁", "Content")
)

@Composable
fun AddEntryScreen(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onGuidedJournalingChoice: (mood: String, isGuided: Boolean, remembersChoice: Boolean, context: String) -> Unit = { _, _, _, _ -> },
) {
    var selectedMoodId by remember { mutableStateOf<String?>(null) }
    var customEmoji by remember { mutableStateOf("") }
    var customLabel by remember { mutableStateOf("Custom") }
    var showCustomEmojiDialog by remember { mutableStateOf(false) }
    var checked by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val entries by authViewModel.entries.collectAsState()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        authViewModel.loadUserEntries()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back Button
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.Start)
                .size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBackIosNew,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Title
        Text(
            text = "How are you feeling today?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Mood Options Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Default Emojis
            defaultMoodOptions.forEach { mood ->
                val isSelected = selectedMoodId == mood.id
                MoodItem(
                    emoji = mood.emoji,
                    label = mood.label,
                    isSelected = isSelected,
                    onClick = {
                        selectedMoodId = mood.id
                    }
                )
            }

            // Custom Emoji Option
            val isCustomSelected = selectedMoodId == "custom"
            MoodItem(
                emoji = customEmoji.ifEmpty { "✨" },
                label = customLabel.ifEmpty { "Custom" },
                isSelected = isCustomSelected,
                isCustom = customEmoji.isEmpty(),
                onClick = {
                    if (customEmoji.isEmpty() || isCustomSelected) {
                        showCustomEmojiDialog = true
                    } else {
                        selectedMoodId = "custom"
                    }
                },
                onLongClick = {
                    showCustomEmojiDialog = true
                }
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Guided Journaling Prompt (revealed on selecting a mood)
        AnimatedVisibility(
            visible = selectedMoodId != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            val selectedMoodLabel = when (selectedMoodId) {
                "custom" -> if (customEmoji.isNotEmpty()) "$customEmoji $customLabel" else "Custom"
                else -> defaultMoodOptions.find { it.id == selectedMoodId }?.let { "${it.emoji} ${it.label}" } ?: ""
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Selected: $selectedMoodLabel",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = { showCustomEmojiDialog = true },
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit custom emoji",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(text = "Change", fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Would you like guided journaling?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Row (
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Checkbox(onCheckedChange = {checked = it}, checked = checked)
                    Text(
                        text = "Remember my choice",
                        modifier = Modifier.align(Alignment.CenterVertically)

                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            val contextTitle = getRandomContextTitle(entries, selectedMoodLabel)
                            onGuidedJournalingChoice(selectedMoodLabel, true, checked, contextTitle)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = "Yes, please!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            onGuidedJournalingChoice(selectedMoodLabel, false, checked, "")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "No, I'll write on my own",
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    // Dialog for adding or editing custom emoji
    if (showCustomEmojiDialog) {
        var tempEmoji by remember { mutableStateOf(customEmoji) }
        var tempLabel by remember { mutableStateOf(if (customLabel == "Custom") "" else customLabel) }

        AlertDialog(
            onDismissRequest = { showCustomEmojiDialog = false },
            title = {
                Text(
                    text = if (customEmoji.isEmpty()) "Custom Mood" else "Edit Custom Mood",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter an emoji and a label for your custom mood:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempEmoji,
                        onValueChange = { tempEmoji = it },
                        label = { Text("Emoji (e.g. 🥳)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempLabel,
                        onValueChange = { tempLabel = it },
                        label = { Text("Label (e.g. Excited)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tempEmoji.isNotBlank()) {
                            customEmoji = tempEmoji.trim()
                            customLabel = if (tempLabel.isNotBlank()) tempLabel.trim() else "Custom"
                            selectedMoodId = "custom"
                        }
                        showCustomEmojiDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomEmojiDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MoodItem(
    emoji: String,
    label: String,
    isSelected: Boolean,
    isCustom: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 8.dp, horizontal = 2.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else Color.Transparent
                )
                .border(
                    width = if (isSelected) 2.dp else 0.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = CircleShape
                )
        ) {
            if (isCustom && emoji == "✨") {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add custom emoji",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            } else {
                Text(
                    text = emoji,
                    fontSize = 28.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AddEntryScreenPreview() {
    StoryworthyTheme {
        AddEntryScreen()
    }
}