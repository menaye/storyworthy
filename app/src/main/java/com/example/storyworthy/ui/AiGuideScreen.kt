package com.example.storyworthy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.storyworthy.Quote
import com.example.storyworthy.ui.theme.StoryworthyTheme

@Composable
fun AiGuideScreen(
    modifier: Modifier = Modifier,
    selectedMood: String = "😊 Content",
    initialQuote: String = "",
    entryContext: String = "",
    viewModel: AiGuideViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onNavigateToWriteEntry: (notes: String) -> Unit = {}
) {
    LaunchedEffect(selectedMood, initialQuote, entryContext) {
        viewModel.loadPromptForMood(selectedMood, initialQuote, entryContext)
    }

    AiGuideContent(
        modifier = modifier,
        selectedMood = selectedMood,
        initialQuote = initialQuote,
        currentPrompt = viewModel.aiPrompt,
        isGenerating = viewModel.isGenerating,
        userNotes = viewModel.userNotes,
        recentEntriesSummary = viewModel.recentEntriesSummary,
        onUserNotesChange = viewModel::onUserNotesChange,
        onRegenerateClick = viewModel::regeneratePrompt,
        onBackClick = onBackClick,
        onNavigateToWriteEntry = { onNavigateToWriteEntry(viewModel.userNotes) }
    )
}

@Composable
fun AiGuideContent(
    modifier: Modifier = Modifier,
    selectedMood: String = "😊 Content",
    initialQuote: String = "",
    currentPrompt: String = "Generating AI journaling question...",
    isGenerating: Boolean = false,
    userNotes: String = "",
    recentEntriesSummary: List<String> = emptyList(),
    onUserNotesChange: (String) -> Unit = {},
    onRegenerateClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onNavigateToWriteEntry: () -> Unit = {}
) {

    val currentQuote = remember(initialQuote) { if (initialQuote.isNotEmpty()) Quote(initialQuote, "") else Quote("No quote selected", "") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back Button & Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Brainstorm Sandbox",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Context Card (Mood, Quote, Recent Entries)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Mood: ", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(text = selectedMood, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                }
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Outlined.FormatQuote,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = currentQuote.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (recentEntriesSummary.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Recent context: ", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text(
                            text = recentEntriesSummary.joinToString(", "),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // AI Agent Chat Bubble / Prompt Box
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = "AI Agent",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Brainstorming Coach",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = currentPrompt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onRegenerateClick,
                        enabled = !isGenerating
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(imageVector = Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isGenerating) "Generating..." else "Regenerate Response", fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // User Brainstorming Notes
        OutlinedTextField(
            value = userNotes,
            onValueChange = onUserNotesChange,
            label = { Text("Jot down thoughts & ideas...") },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Navigate to Write Entry Screen Button
        Button(
            onClick = onNavigateToWriteEntry,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("I'm ready — Go to Write Entry", fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AiGuideScreenPreview() {
    StoryworthyTheme {
        AiGuideContent()
    }
}
