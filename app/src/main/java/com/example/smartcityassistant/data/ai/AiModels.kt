package com.example.smartcityassistant.data.ai

data class AiMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val actionCard: AiActionCardData? = null
)

data class AiActionCardData(
    val title: String,
    val description: String,
    val actionText: String = "Open",
    val intent: String? = null,
    val action: String? = null,
    val parameters: Map<String, String>? = null
)

data class AiResponse(
    val reply: String,
    val intent: String? = null,
    val action: String? = null,
    val parameters: Map<String, String>? = null,
    val actionCard: AiActionCardData? = null
)
