package com.example.smartcityassistant.ui.government.schemes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.smartcityassistant.data.government.schemes.SchemeCategory
import com.example.smartcityassistant.data.government.schemes.SchemeIconType

object GovernmentSchemeStyleResolver {
    fun getCategoryColor(category: SchemeCategory): Color {
        return when (category) {
            SchemeCategory.STUDENTS_EDUCATION -> Color(0xFF00ACC1) // Cyan / Education Blue
            SchemeCategory.FARMERS_AGRICULTURE -> Color(0xFF43A047) // Green / Earth Tone
            SchemeCategory.WOMEN_CHILDREN -> Color(0xFFE91E63) // Pink / Rose
            SchemeCategory.HEALTH -> Color(0xFF00897B) // Teal / Medical Green
            SchemeCategory.HOUSING -> Color(0xFFEF6C00) // Orange / Warm
            SchemeCategory.EMPLOYMENT_SKILL -> Color(0xFF1E88E5) // Blue
            SchemeCategory.FINANCIAL_ASSISTANCE -> Color(0xFF388E3C) // Green
            SchemeCategory.PENSION_SENIOR_CITIZENS -> Color(0xFF6D4C41) // Warm Neutral / Brown
            SchemeCategory.DISABILITY -> Color(0xFF7B1FA2) // Purple / Accessible Violet
            SchemeCategory.BUSINESS_STARTUP -> Color(0xFF3F51B5) // Indigo
            SchemeCategory.BANKING_INSURANCE -> Color(0xFF2E7D32) // Green
            SchemeCategory.SOCIAL_SECURITY -> Color(0xFF1976D2) // Blue
            SchemeCategory.FOOD_NUTRITION -> Color(0xFFF57C00) // Orange
            else -> Color(0xFF0D2B4E) // Neutral Navy
        }
    }

    fun getSchemeIcon(iconType: SchemeIconType): ImageVector {
        return when (iconType) {
            SchemeIconType.SCHOOL -> Icons.Default.School
            SchemeIconType.AGRICULTURE -> Icons.Default.Agriculture
            SchemeIconType.FEMALE -> Icons.Default.Female
            SchemeIconType.HEALTH -> Icons.Default.MedicalServices
            SchemeIconType.HOME -> Icons.Default.Home
            SchemeIconType.WORK -> Icons.Default.Work
            SchemeIconType.PAYMENTS -> Icons.Default.Payments
            SchemeIconType.ELDERLY -> Icons.Default.Elderly
            SchemeIconType.ACCESSIBLE -> Icons.Default.Accessible
            SchemeIconType.BUSINESS -> Icons.Default.Business
            SchemeIconType.ACCOUNT_BALANCE -> Icons.Default.AccountBalance
            SchemeIconType.SHIELD -> Icons.Default.Shield
            SchemeIconType.RESTAURANT -> Icons.Default.Restaurant
            else -> Icons.Default.Info
        }
    }
}
