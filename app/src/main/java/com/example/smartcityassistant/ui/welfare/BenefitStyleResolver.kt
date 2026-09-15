package com.example.smartcityassistant.ui.welfare

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.smartcityassistant.data.welfare.WelfareCategory
import com.example.smartcityassistant.data.welfare.WelfareIconType

object BenefitStyleResolver {
    fun getCategoryColor(category: WelfareCategory): Color {
        return when (category) {
            WelfareCategory.WOMEN_CHILDREN -> Color(0xFFE91E63) // Pink / Rose
            WelfareCategory.SENIOR_CITIZENS -> Color(0xFF6D4C41) // Warm Neutral / Brown
            WelfareCategory.HEALTH -> Color(0xFF00897B) // Teal / Medical Green
            WelfareCategory.DISABILITY -> Color(0xFF7B1FA2) // Purple / Accessible Violet
            WelfareCategory.STUDENTS -> Color(0xFF00ACC1) // Cyan / Education Blue
            WelfareCategory.FARMERS -> Color(0xFF43A047) // Green / Earth Tone
            WelfareCategory.EMPLOYMENT -> Color(0xFF1E88E5) // Blue
            WelfareCategory.HOUSING -> Color(0xFFEF6C00) // Orange / Warm
            WelfareCategory.FINANCIAL_ASSISTANCE -> Color(0xFF388E3C) // Green
            WelfareCategory.SOCIAL_SECURITY -> Color(0xFF1976D2) // Blue
            else -> Color(0xFF0D2B4E) // Neutral Navy
        }
    }

    fun getBenefitIcon(iconType: WelfareIconType): ImageVector {
        return when (iconType) {
            WelfareIconType.FEMALE -> Icons.Default.Female
            WelfareIconType.ELDERLY -> Icons.Default.Elderly
            WelfareIconType.HEALTH -> Icons.Default.MedicalServices
            WelfareIconType.ACCESSIBLE -> Icons.Default.Accessibility
            WelfareIconType.SCHOOL -> Icons.Default.School
            WelfareIconType.AGRICULTURE -> Icons.Default.Agriculture
            WelfareIconType.WORK -> Icons.Default.Work
            WelfareIconType.HOME -> Icons.Default.Home
            WelfareIconType.PAYMENTS -> Icons.Default.Payments
            WelfareIconType.SHIELD -> Icons.Default.Shield
            else -> Icons.Default.Info
        }
    }
}
