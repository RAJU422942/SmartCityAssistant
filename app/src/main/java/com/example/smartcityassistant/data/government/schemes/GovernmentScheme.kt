package com.example.smartcityassistant.data.government.schemes

data class GovernmentScheme(
    val id: String,
    val name: String,
    val shortDescription: String,
    val purpose: String,
    val category: SchemeCategory,
    val beneficiary: String,
    val eligibility: String,
    val benefits: String,
    val documents: List<String>,
    val howToApply: String,
    val authority: String,
    val officialUrl: String?,
    val keywords: List<String>,
    val iconType: SchemeIconType,
    val verificationStatus: VerificationStatus = VerificationStatus.VERIFIED,
    val lastVerified: String = "2026-09",
    val isNational: Boolean = true
)
