package com.example.smartcityassistant.ui.government.helplines

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.smartcityassistant.data.government.helplines.HelplineCategory

object HelplineStyleResolver {
    fun getCategoryColor(category: HelplineCategory): Color {
        return when (category) {
            HelplineCategory.EMERGENCY -> Color(0xFFD32F2F) // Red
            HelplineCategory.POLICE_SAFETY -> Color(0xFF1E88E5) // Blue
            HelplineCategory.CYBER_CRIME -> Color(0xFF3F51B5) // Indigo / Security Purple
            HelplineCategory.WOMEN_CHILDREN -> Color(0xFFE91E63) // Pink / Rose
            HelplineCategory.HEALTH -> Color(0xFF00897B) // Teal / Medical Green
            HelplineCategory.DISASTER -> Color(0xFFEF6C00) // Orange / Amber
            HelplineCategory.STUDENTS -> Color(0xFF00ACC1) // Cyan / Education Blue
            HelplineCategory.CONSUMER -> Color(0xFF2E7D32) // Green
            HelplineCategory.LEGAL -> Color(0xFF512DA8) // Deep Purple
            HelplineCategory.TRANSPORT -> Color(0xFF1976D2) // Blue
            HelplineCategory.IDENTITY -> Color(0xFF303F9F) // Indigo
            HelplineCategory.FINANCE -> Color(0xFF388E3C) // Green
            HelplineCategory.FARMERS -> Color(0xFF43A047) // Earth / Green
            HelplineCategory.SENIOR_CITIZENS -> Color(0xFF6D4C41) // Brown / Warm Neutral
            HelplineCategory.DISABILITY -> Color(0xFF7B1FA2) // Accessible Violet
            else -> Color(0xFF0D2B4E) // Neutral Navy
        }
    }

    fun getCategoryIcon(category: HelplineCategory): ImageVector {
        return when (category) {
            HelplineCategory.EMERGENCY -> Icons.Default.Emergency
            HelplineCategory.POLICE_SAFETY -> Icons.Default.LocalPolice
            HelplineCategory.CYBER_CRIME -> Icons.Default.Security
            HelplineCategory.WOMEN_CHILDREN -> Icons.Default.Female
            HelplineCategory.HEALTH -> Icons.Default.MedicalServices
            HelplineCategory.DISASTER -> Icons.Default.Warning
            HelplineCategory.STUDENTS -> Icons.Default.School
            HelplineCategory.CONSUMER -> Icons.Default.ShoppingCart
            HelplineCategory.LEGAL -> Icons.Default.Gavel
            HelplineCategory.TRANSPORT -> Icons.Default.Train
            HelplineCategory.IDENTITY -> Icons.Default.Badge
            HelplineCategory.FINANCE -> Icons.Default.AccountBalance
            HelplineCategory.FARMERS -> Icons.Default.Agriculture
            HelplineCategory.SENIOR_CITIZENS -> Icons.Default.Elderly
            HelplineCategory.DISABILITY -> Icons.Default.Accessible
            else -> Icons.Default.Info
        }
    }
}
