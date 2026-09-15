package com.example.smartcityassistant.data.government

data class GovScheme(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val eligibility: String,
    val officialSource: String,
    val officialUrl: String
)

data class WelfareBenefit(
    val id: String,
    val name: String,
    val description: String,
    val eligibility: String,
    val requiredDocuments: List<String>,
    val howToApply: String,
    val officialSource: String
)

data class GovNotice(
    val id: String,
    val title: String,
    val summary: String,
    val date: String,
    val authority: String,
    val sourceUrl: String
)

data class GovHelpline(
    val id: String,
    val name: String,
    val number: String,
    val purpose: String,
    val category: String
)
