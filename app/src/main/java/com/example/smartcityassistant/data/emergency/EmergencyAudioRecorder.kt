package com.example.smartcityassistant.data.emergency

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class EmergencyAudioRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var rawAudioFile: File? = null
    private var stableAudioFile: File? = null

    fun startRecording(): File? {
        try {
            cancelRecording()
            rawAudioFile = File(context.cacheDir, "emergency_raw_${System.currentTimeMillis()}.m4a")
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(rawAudioFile!!.absolutePath)
                prepare()
                start()
            }
            Log.d("EmergencyAudioDebug", "MediaRecorder started successfully, path=${rawAudioFile!!.absolutePath}")
            return rawAudioFile
        } catch (e: Exception) {
            Log.e("EmergencyAudioDebug", "Failed to start MediaRecorder", e)
            cancelRecording()
            return null
        }
    }

    fun stopRecording(): File? {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: RuntimeException) {
            Log.e("EmergencyAudioDebug", "RuntimeException in MediaRecorder.stop() (often recording too short)", e)
            try {
                rawAudioFile?.delete()
            } catch (ex: Exception) {}
            rawAudioFile = null
        } catch (e: Exception) {
            Log.e("EmergencyAudioDebug", "Exception in MediaRecorder stop/release", e)
        }
        mediaRecorder = null

        if (rawAudioFile != null && rawAudioFile!!.exists() && rawAudioFile!!.length() > 0) {
            try {
                val emergencyDir = File(context.filesDir, "emergency_audio")
                if (!emergencyDir.exists()) {
                    emergencyDir.mkdirs()
                }
                stableAudioFile = File(emergencyDir, "emergency_voice_${System.currentTimeMillis()}.m4a")
                
                FileInputStream(rawAudioFile!!).use { input ->
                    FileOutputStream(stableAudioFile!!).use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d("EmergencyAudioDebug", "Stable audio file created: path=${stableAudioFile!!.absolutePath}, size=${stableAudioFile!!.length()}")
            } catch (e: Exception) {
                Log.e("EmergencyAudioDebug", "Failed to copy raw audio to stable file", e)
                stableAudioFile = rawAudioFile
            }
        } else {
            Log.w("EmergencyAudioDebug", "Raw audio file does not exist or has 0 bytes")
            stableAudioFile = null
        }
        return stableAudioFile
    }

    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {}
        mediaRecorder = null
        try {
            rawAudioFile?.delete()
        } catch (e: Exception) {}
        rawAudioFile = null
        stableAudioFile = null
    }

    fun getAudioUri(file: File): Uri? {
        return try {
            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        } catch (e: Exception) {
            null
        }
    }
}
