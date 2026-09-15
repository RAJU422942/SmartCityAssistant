package com.example.smartcityassistant.data.government.schemes

enum class SchemeCategory(val displayName: String) {
    ALL("All"),
    STUDENTS_EDUCATION("Students & Education"),
    FARMERS_AGRICULTURE("Farmers & Agriculture"),
    WOMEN_CHILDREN("Women & Children"),
    HEALTH("Health"),
    HOUSING("Housing"),
    EMPLOYMENT_SKILL("Employment & Skill"),
    FINANCIAL_ASSISTANCE("Financial Assistance"),
    PENSION_SENIOR_CITIZENS("Pension & Senior Citizens"),
    DISABILITY("Disability"),
    BUSINESS_STARTUP("Business & Startup"),
    BANKING_INSURANCE("Banking & Insurance"),
    SOCIAL_SECURITY("Social Security"),
    FOOD_NUTRITION("Food & Nutrition"),
    OTHER("Other")
}

enum class SchemeIconType {
    SCHOOL,
    AGRICULTURE,
    FEMALE,
    HEALTH,
    HOME,
    WORK,
    PAYMENTS,
    ELDERLY,
    ACCESSIBLE,
    BUSINESS,
    ACCOUNT_BALANCE,
    SHIELD,
    RESTAURANT,
    INFO
}

enum class VerificationStatus {
    VERIFIED, UNVERIFIED
}
