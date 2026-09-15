package com.example.smartcityassistant.data.ai

import java.io.IOException
import retrofit2.HttpException

class AiRepository(private val apiService: AiApiService = AiApiClient.service) {
    suspend fun sendMessage(message: String): Result<AiChatData> {
        return try {
            val response = apiService.chat(AiChatRequest(message))
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                val errorMsg = response.error?.message ?: "Smart City AI is temporarily unavailable. Please try again."
                Result.failure(Exception(errorMsg))
            }
        } catch (e: HttpException) {
            Result.failure(Exception("Smart City AI is temporarily unavailable. Please try again."))
        } catch (e: IOException) {
            Result.failure(Exception("Unable to connect right now. Please check your internet connection and try again."))
        } catch (e: Exception) {
            Result.failure(Exception("I couldn't process that request. Please try again."))
        }
    }
}
