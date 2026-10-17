package com.example.storyworthy.ui.theme

import android.util.Log
import com.google.adk.kt.annotations.Param
import com.google.adk.kt.annotations.Tool
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class BrainstormService {
    companion object {
        private const val TAG = "BrainstormService"
    }

    private val generativeModel by lazy {
        try {
            Firebase.ai(backend = GenerativeBackend.googleAI())
                .generativeModel(
                    modelName = "gemini-3.8-flash",
                    systemInstruction = content {
                        text("You are an inspiring journaling coach. Provide 1 concise, engaging question (15-20 words max) tailored to the user's mood and entry context. Do not include introductory filler.")
                    },
                    generationConfig = generationConfig {
                        maxOutputTokens = 1000
                        temperature = 0.7f
                        thinkingConfig = thinkingConfig {
                            thinkingLevel = ThinkingLevel.LOW
                        }
                    }
                )
        } catch (_: Exception) {
            null
        }
    }

    /** Takes mood and previous entries as inputs to dynamically generate a thoughtful journaling question */
    @Tool
    fun suggestJournalTopic(
        @Param("The current mood of the user") mood: String,
        @Param("A list of the user's recent journal entries") previousEntries: List<String>
    ): String {
        val lastEntry = previousEntries.lastOrNull() ?: "your recent reflections"
        return "User Mood: $mood. Context: '$lastEntry'."
    }

    /** Streams AI-guided journaling question response in real-time for instant feedback */
    fun generateAiQuestionStream(mood: String, previousEntries: List<String>): Flow<String> = flow {
        val prompt = suggestJournalTopic(mood, previousEntries)
        val model = generativeModel
        if (model == null) {
            emit("Reflect on what contributed to feeling $mood today, and what you're hoping for next.")
            return@flow
        }

        try {
            var fullText = ""
            model.generateContentStream(prompt).collect { chunk ->
                chunk.text?.let { text ->
                    fullText += text
                    emit(fullText)
                }
            }
            if (fullText.isBlank()) {
                emit("Reflect on what contributed to feeling $mood today, and what you're hoping for next.")
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Error generating AI question stream: ${e.message}", e)
            emit("Reflect on what contributed to feeling $mood today, and what you're hoping for next.")
        }
    }

}
