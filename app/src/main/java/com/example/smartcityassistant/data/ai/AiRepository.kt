package com.example.smartcityassistant.data.ai

import android.util.Log
import java.io.IOException
import retrofit2.HttpException

class AiRepository(private val apiService: AiApiService = AiApiClient.service) {
    companion object {
        private const val TAG = "AiRepository"
    }

    suspend fun sendMessage(message: String): Result<AiChatData> {
        return try {
            val response = apiService.chat(AiChatRequest(message))
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                val errorCode = response.error?.code ?: "UNKNOWN"
                val errorMsg = response.error?.message ?: "Smart City AI is temporarily unavailable. Please try again."
                Log.w(TAG, "API failure response: code=$errorCode, message=$errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: HttpException) {
            val code = e.code()
            val url = e.response()?.raw()?.request?.url?.toString() ?: "unknown"
            Log.e(TAG, "HttpException: status=$code, url=$url", e)
            val friendlyMsg = when (code) {
                400 -> "Invalid request. Please try again."
                404 -> "AI service endpoint not found."
                500 -> "Server error occurred. Please try again later."
                503 -> "Smart City AI is temporarily unavailable. Please try again."
                else -> "Smart City AI is temporarily unavailable. Please try again."
            }
            Result.failure(Exception(friendlyMsg))
        } catch (e: IOException) {
            Log.e(TAG, "IOException (Timeout/Network): ${e.javaClass.simpleName} - ${e.message}", e)
            Result.failure(Exception("Unable to connect right now. Please check your internet connection and try again."))
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.javaClass.name} - ${e.message}", e)
            Result.failure(Exception("I couldn't process that request. Please try again."))
        }
    }
}
