package com.example.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Tool
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

sealed interface AiStudyState {
    data object Idle : AiStudyState
    data object Loading : AiStudyState
    data class Success(
        val content: String,
        val groundingType: String
    ) : AiStudyState
    data class Error(val message: String) : AiStudyState
}

/**
 * ViewModel managing AI study assistance using Gemini 3.8 Flash
 * with Search Grounding (googleSearch) and Maps Grounding (googleMaps).
 */
class AiStudyViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<AiStudyState>(AiStudyState.Idle)
    val uiState: StateFlow<AiStudyState> = _uiState.asStateFlow()

    // 1. Model with Search Grounding (googleSearch tool)
    private val searchGroundingModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            tools = listOf(Tool.googleSearch())
        )
    }

    // 2. Model with Maps Grounding (googleMaps tool)
    private val mapsGroundingModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            tools = listOf(Tool.googleMaps())
        )
    }

    /**
     * Research a study topic or academic question using real-time Google Search data.
     */
    fun researchWithGoogleSearch(topicOrQuestion: String) {
        if (topicOrQuestion.isBlank()) return

        viewModelScope.launch {
            _uiState.value = AiStudyState.Loading
            try {
                val prompt = "Provide an accurate, up-to-date study explanation and breakdown for: $topicOrQuestion. Include key academic facts, citations or relevant references where applicable."
                val response = withTimeout(45_000L) {
                    searchGroundingModel.generateContent(prompt)
                }
                val text = response.text
                if (!text.isNullOrBlank()) {
                    _uiState.value = AiStudyState.Success(
                        content = text,
                        groundingType = "Google Search Grounded"
                    )
                } else {
                    _uiState.value = AiStudyState.Error("Model returned an empty explanation. Please try again.")
                }
            } catch (e: TimeoutCancellationException) {
                _uiState.value = AiStudyState.Error("Search research timed out. Please verify connectivity and try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = AiStudyState.Error(e.localizedMessage ?: "Failed to generate search grounded response.")
            }
        }
    }

    /**
     * Find real-world study spots, libraries, quiet cafes, or campus labs using Google Maps data.
     */
    fun findStudySpotsWithMaps(locationOrQuery: String) {
        if (locationOrQuery.isBlank()) return

        viewModelScope.launch {
            _uiState.value = AiStudyState.Loading
            try {
                val prompt = "Find top-rated quiet study spaces, public or university libraries, and study-friendly cafes near or in: $locationOrQuery. Provide location names, addresses/areas, opening vibes, and suitability for focused study."
                val response = withTimeout(45_000L) {
                    mapsGroundingModel.generateContent(prompt)
                }
                val text = response.text
                if (!text.isNullOrBlank()) {
                    _uiState.value = AiStudyState.Success(
                        content = text,
                        groundingType = "Google Maps Grounded"
                    )
                } else {
                    _uiState.value = AiStudyState.Error("No study spots could be found. Please try refining your query.")
                }
            } catch (e: TimeoutCancellationException) {
                _uiState.value = AiStudyState.Error("Maps search timed out. Please try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = AiStudyState.Error(e.localizedMessage ?: "Failed to query Google Maps data.")
            }
        }
    }

    fun resetState() {
        _uiState.value = AiStudyState.Idle
    }
}
