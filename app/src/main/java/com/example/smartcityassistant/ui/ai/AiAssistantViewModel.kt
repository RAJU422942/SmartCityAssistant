package com.example.smartcityassistant.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartcityassistant.data.ai.AiActionCardData
import com.example.smartcityassistant.data.ai.AiMessage
import com.example.smartcityassistant.data.ai.AiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class AiAssistantViewModel(
    private val aiRepository: AiRepository = AiRepository()
) : ViewModel() {

    private val _messages = MutableStateFlow<List<AiMessage>>(emptyList())
    val messages: StateFlow<List<AiMessage>> = _messages.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _isLoading.value) return
        val userMsg = AiMessage(
            id = UUID.randomUUID().toString(),
            text = text.trim(),
            isUser = true
        )
        _messages.update { it + userMsg }
        _inputText.value = ""
        _errorMessage.value = null

        viewModelScope.launch {
            _isLoading.value = true
            val result = aiRepository.sendMessage(text.trim())
            _isLoading.value = false

            result.fold(
                onSuccess = { data ->
                    val actionCardData = buildActionCard(data.action, data.getStringParameters())
                    val assistantMsg = AiMessage(
                        id = UUID.randomUUID().toString(),
                        text = data.reply,
                        isUser = false,
                        actionCard = actionCardData
                    )
                    _messages.update { it + assistantMsg }
                },
                onFailure = { e ->
                    _errorMessage.value = e.localizedMessage ?: "Something went wrong. Please try again."
                }
            )
        }
    }

    private fun buildActionCard(action: String?, parameters: Map<String, String>?): AiActionCardData? {
        if (action.isNullOrBlank()) return null
        val (title, description, actionText) = when (action) {
            "OPEN_EMERGENCY" -> Triple("Emergency Services", "Access emergency helplines & SOS", "Open Emergency")
            "OPEN_AQI" -> Triple("Air Quality (AQI)", "Check live AQI and pollutant details", "Check AQI")
            "OPEN_NEARBY" -> Triple("Nearby Essential Services", "Find hospitals, police & services near you", "Find Nearby")
            "OPEN_TRANSPORT" -> Triple("City Transport", "Bus, rail & route planning", "Open Transport")
            "OPEN_RAILWAY" -> Triple("Railway Services", "Train schedules, PNR & live status", "Open Railway")
            "OPEN_GOVERNMENT" -> Triple("Government Services", "Official portals & online services", "Open Government")
            "OPEN_SCHEMES" -> Triple("Government Schemes", "Welfare schemes & benefits", "View Schemes")
            "OPEN_REPORT_PROBLEM" -> Triple("Report a Problem", "Report garbage, roads, water issues", "Report Problem")
            "OPEN_MY_COMPLAINTS" -> Triple("My Complaints", "Track status of submitted reports", "View Complaints")
            "OPEN_CITY_ALERTS" -> Triple("City Alerts", "Local advisories & notifications", "View Alerts")
            else -> return null
        }
        return AiActionCardData(
            title = title,
            description = description,
            actionText = actionText,
            action = action,
            parameters = parameters
        )
    }

    fun retryLastAction() {
        _errorMessage.value = null
        val lastUserMessage = _messages.value.lastOrNull { it.isUser }
        if (lastUserMessage != null) {
            sendMessage(lastUserMessage.text)
        }
    }

    fun setError(message: String?) {
        _errorMessage.value = message
    }
}
