package com.example.smartcityassistant.data.emergency

object EmergencyVoiceClassifier {
    fun classify(speechText: String): EmergencyType {
        val lower = speechText.lowercase()

        val medicalKeywords = listOf("doctor", "hospital", "ambulance", "injured", "injury", "bleeding", "heart", "breathing", "unconscious", "medical", "sick", "tabiyat", "chot", "khoon", "patient", "bimari")
        val policeKeywords = listOf("police", "threat", "danger", "attack", "robbery", "crime", "unsafe", "help me", "police help", "maar raha", "dhamki", "chor", "loot")
        val fireKeywords = listOf("fire", "burning", "smoke", "flames", "aag", "dhuaan", "jal raha", "aag lag gayi")
        val accidentKeywords = listOf("accident", "crash", "collision", "bike accident", "car accident", "road accident", "vehicle accident", "takra", "accident ho gaya", "gadi lad gayi")
        val disasterKeywords = listOf("flood", "earthquake", "storm", "cyclone", "landslide", "disaster", "flooding", "baarish se flood", "bhukamp", "toofan", "aandhi")

        val medicalScore = medicalKeywords.count { lower.contains(it) }
        val policeScore = policeKeywords.count { lower.contains(it) }
        val fireScore = fireKeywords.count { lower.contains(it) }
        val accidentScore = accidentKeywords.count { lower.contains(it) }
        val disasterScore = disasterKeywords.count { lower.contains(it) }

        val maxScore = maxOf(medicalScore, policeScore, fireScore, accidentScore, disasterScore)
        if (maxScore == 0) return EmergencyType.OTHER

        return when (maxScore) {
            accidentScore -> EmergencyType.ACCIDENT
            medicalScore -> EmergencyType.MEDICAL
            policeScore -> EmergencyType.POLICE
            fireScore -> EmergencyType.FIRE
            disasterScore -> EmergencyType.DISASTER
            else -> EmergencyType.OTHER
        }
    }
}
