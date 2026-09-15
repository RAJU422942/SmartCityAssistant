package com.example.smartcityassistant.data.welfare

enum class WelfareCategory(val displayName: String) {
    ALL("All"),
    WOMEN_CHILDREN("Women & Children"),
    SENIOR_CITIZENS("Senior Citizens"),
    HEALTH("Health"),
    DISABILITY("Disability"),
    STUDENTS("Students"),
    FARMERS("Farmers"),
    EMPLOYMENT("Employment"),
    HOUSING("Housing"),
    FINANCIAL_ASSISTANCE("Financial Assistance"),
    SOCIAL_SECURITY("Social Security"),
    OTHER("Other")
}

enum class WelfareIconType {
    FEMALE,
    ELDERLY,
    HEALTH,
    ACCESSIBLE,
    SCHOOL,
    AGRICULTURE,
    WORK,
    HOME,
    PAYMENTS,
    SHIELD,
    INFO
}

data class GovernmentBenefit(
    val id: String,
    val name: String,
    val category: WelfareCategory,
    val shortDescription: String,
    val about: String,
    val keyBenefit: String,
    val eligibility: String,
    val requiredDocuments: List<String>,
    val howToApply: String,
    val authority: String,
    val officialUrl: String?,
    val lastVerified: String = "2026-09",
    val keywords: List<String>,
    val iconType: WelfareIconType
)
