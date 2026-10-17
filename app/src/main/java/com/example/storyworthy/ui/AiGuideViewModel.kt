package com.example.storyworthy.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storyworthy.ui.theme.BrainstormService
import kotlinx.coroutines.launch

class AiGuideViewModel : ViewModel() {
    private val brainstormService = BrainstormService()

    var recentEntriesSummary by mutableStateOf<List<String>>(emptyList())
        private set

    var currentMood by mutableStateOf<String?>(null)
        private set

    var aiPrompt by mutableStateOf("Generating AI journaling question...")
        private set

    var isGenerating by mutableStateOf(false)
        private set

    var userNotes by mutableStateOf("")
        private set

    private fun getInstantMoodStarter(mood: String): String {
        return when {
            mood.contains("happy", ignoreCase = true) || mood.contains("content", ignoreCase = true) ->
                "What specific moment or interaction brought you warmth and joy today?"
            mood.contains("sad", ignoreCase = true) || mood.contains("awful", ignoreCase = true) ->
                "What feels heaviest on your mind right now, and what small step could bring comfort?"
            else ->
                "What was the most significant experience or thought that shaped your day?"
        }
    }

    fun loadPromptForMood(mood: String, initialQuote: String = "", entryContext: String = "") {
        if (entryContext.isNotBlank()) {
            recentEntriesSummary = listOf(entryContext)
        }

        if (initialQuote.isNotBlank() && (userNotes.isBlank() || mood != currentMood)) {
            userNotes = initialQuote
        }

        if (mood == currentMood && aiPrompt.isNotBlank() && aiPrompt != "Generating AI journaling question...") {
            return
        }

        if (mood != currentMood) {
            currentMood = mood
            if (initialQuote.isBlank()) {
                userNotes = ""
            }
        }

        viewModelScope.launch {
            isGenerating = true
            aiPrompt = getInstantMoodStarter(mood)
            brainstormService.generateAiQuestionStream(mood, recentEntriesSummary).collect { textChunk ->
                if (textChunk.isNotBlank()) {
                    aiPrompt = textChunk
                }
            }
            isGenerating = false
        }
    }

    fun regeneratePrompt() {
        val mood = currentMood ?: return
        viewModelScope.launch {
            isGenerating = true
            aiPrompt = getInstantMoodStarter(mood)
            brainstormService.generateAiQuestionStream(mood, recentEntriesSummary.shuffled()).collect { textChunk ->
                if (textChunk.isNotBlank()) {
                    aiPrompt = textChunk
                }
            }
            isGenerating = false
        }
    }

    fun onUserNotesChange(newNotes: String) {
        userNotes = newNotes
    }
}
