package com.example.smartcityassistant.data.notices

enum class NoticeCategory(val displayName: String) {
    ALL("All"),
    CIVIC("Civic"),
    ENVIRONMENT("Environment"),
    WEATHER("Weather"),
    DISASTER("Disaster"),
    TRANSPORT("Transport"),
    CONSUMER("Consumer"),
    CYBER_SAFETY("Cyber Safety"),
    HEALTH("Health"),
    EDUCATION("Education"),
    AGRICULTURE("Agriculture"),
    CITIZEN_SERVICES("Citizen Services")
}

enum class NoticePriority {
    NORMAL, IMPORTANT, URGENT
}

enum class NoticeIconType {
    CIVIC,
    ENVIRONMENT,
    WEATHER,
    DISASTER,
    TRANSPORT,
    CONSUMER,
    CYBER_SAFETY,
    HEALTH,
    EDUCATION,
    AGRICULTURE,
    CITIZEN_SERVICES
}

data class GovernmentNotice(
    val id: String,
    val title: String,
    val category: NoticeCategory,
    val date: String,
    val shortDescription: String,
    val fullDescription: String,
    val authority: String,
    val officialUrl: String?,
    val lastVerified: String = "2026-09",
    val priority: NoticePriority = NoticePriority.NORMAL,
    val keywords: List<String>,
    val iconType: NoticeIconType
)
