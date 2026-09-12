package com.example.smartcityassistant.aqi

import androidx.compose.ui.graphics.Color

data class AqiStyle(
    val category: String,
    val backgroundColor: Color,
    val textColor: Color
)

object AqiClassification {
    fun getStyle(aqi: Int): AqiStyle {
        return when {
            aqi <= 50 -> AqiStyle("Good", Color(0xFF4CAF50), Color.White)
            aqi <= 100 -> AqiStyle("Moderate", Color(0xFFFFEB3B), Color(0xFF333333))
            aqi <= 150 -> AqiStyle("Unhealthy for Sensitive Groups", Color(0xFFFF9800), Color.White)
            aqi <= 200 -> AqiStyle("Unhealthy", Color(0xFFF44336), Color.White)
            aqi <= 300 -> AqiStyle("Very Unhealthy", Color(0xFF9C27B0), Color.White)
            else -> AqiStyle("Hazardous", Color(0xFF7E0023), Color.White)
        }
    }

    fun getAdvisory(aqi: Int): String {
        return when {
            aqi <= 50 -> "Air quality is good. Enjoy normal outdoor activities."
            aqi <= 100 -> "Air quality is acceptable. Sensitive individuals should monitor symptoms."
            aqi <= 150 -> "Sensitive groups should reduce prolonged or heavy outdoor activity."
            aqi <= 200 -> "Everyone may experience health effects. Consider reducing prolonged outdoor activity."
            aqi <= 300 -> "Health alert: avoid prolonged outdoor activity, especially for sensitive groups."
            else -> "Health emergency conditions may affect everyone. Avoid outdoor exposure where possible."
        }
    }
}
