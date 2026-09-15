package com.example.smartcityassistant.data.ai

data class AiChatRequest(
    val message: String
)

data class AiChatResponse(
    val success: Boolean,
    val data: AiChatData?,
    val error: AiChatError? = null
)

data class AiChatData(
    val reply: String,
    val intent: String,
    val action: String?,
    val parameters: Map<String, Any?>?
) {
    fun getStringParameters(): Map<String, String>? {
        return parameters?.mapValues { it.value?.toString() ?: "" }
    }
}

data class AiChatError(
    val code: String,
    val message: String
)
