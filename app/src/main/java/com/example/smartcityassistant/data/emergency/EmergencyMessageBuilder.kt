package com.example.smartcityassistant.data.emergency

object EmergencyMessageBuilder {
    fun buildMessage(type: EmergencyType, latitude: Double?, longitude: Double?, voiceText: String? = null): String {
        val locationText = if (latitude != null && longitude != null) {
            "My current location:\nhttps://maps.google.com/?q=$latitude,$longitude"
        } else {
            "My current location: Location unavailable"
        }

        val voiceSection = if (!voiceText.isNullOrBlank()) {
            "What I said:\n\"$voiceText\"\n\n"
        } else {
            ""
        }

        val situationHeading = when (type) {
            EmergencyType.MEDICAL -> "🚑 MEDICAL EMERGENCY ALERT\n\nI am facing a Medical Emergency and may need immediate medical assistance."
            EmergencyType.POLICE -> "🚔 POLICE / THREAT ALERT\n\nI need Police assistance due to an immediate safety/threat situation."
            EmergencyType.FIRE -> "🔥 FIRE EMERGENCY ALERT\n\nI am facing a Fire Emergency and need immediate fire/rescue assistance."
            EmergencyType.ACCIDENT -> "🚗 ROAD ACCIDENT ALERT\n\nA road accident has occurred and medical/emergency assistance may be required."
            EmergencyType.DISASTER -> "🌊 DISASTER EMERGENCY ALERT\n\nI am currently facing a Disaster Emergency and may need immediate assistance."
            EmergencyType.OTHER -> "🆘 EMERGENCY ALERT\n\nI need immediate emergency assistance."
        }

        return "$situationHeading\n\n${voiceSection}$locationText\n\nPlease contact me or arrange emergency assistance immediately.\n\nSent from Smart City Assistant."
    }
}
