package com.example.smartcityassistant.data.government.helplines

enum class HelplineCategory(val displayName: String) {
    ALL("All"),
    EMERGENCY("Emergency"),
    POLICE_SAFETY("Police & Safety"),
    CYBER_CRIME("Cyber Crime"),
    WOMEN_CHILDREN("Women & Children"),
    HEALTH("Health"),
    DISASTER("Disaster"),
    STUDENTS("Students"),
    CONSUMER("Consumer"),
    LEGAL("Legal"),
    TRANSPORT("Transport"),
    IDENTITY("Identity"),
    FINANCE("Finance"),
    FARMERS("Farmers"),
    SENIOR_CITIZENS("Senior Citizens"),
    DISABILITY("Disability"),
    OTHER("Other")
}

enum class HelplinePriority {
    CRITICAL, HIGH, NORMAL
}

data class ImportantHelpline(
    val id: String,
    val name: String,
    val number: String,
    val purpose: String,
    val shortDescription: String,
    val category: HelplineCategory,
    val authority: String,
    val keywords: List<String>,
    val source: String,
    val lastVerified: String = "2026-09",
    val isEmergency: Boolean = false,
    val priority: HelplinePriority = HelplinePriority.NORMAL,
    val officialPortalUrl: String? = null
)
