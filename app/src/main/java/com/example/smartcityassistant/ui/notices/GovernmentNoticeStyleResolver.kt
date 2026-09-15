package com.example.smartcityassistant.ui.notices

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.smartcityassistant.data.notices.NoticeCategory
import com.example.smartcityassistant.data.notices.NoticeIconType

object GovernmentNoticeStyleResolver {
    fun getCategoryColor(category: NoticeCategory): Color {
        return when (category) {
            NoticeCategory.CIVIC -> Color(0xFF1E88E5) // Blue
            NoticeCategory.ENVIRONMENT -> Color(0xFF2E7D32) // Green
            NoticeCategory.WEATHER -> Color(0xFF00ACC1) // Cyan
            NoticeCategory.DISASTER -> Color(0xFFD32F2F) // Red / Urgent
            NoticeCategory.TRANSPORT -> Color(0xFF3F51B5) // Indigo
            NoticeCategory.CONSUMER -> Color(0xFF43A047) // Green
            NoticeCategory.CYBER_SAFETY -> Color(0xFF512DA8) // Deep Purple
            NoticeCategory.HEALTH -> Color(0xFF00897B) // Teal
            NoticeCategory.EDUCATION -> Color(0xFF00ACC1) // Cyan
            NoticeCategory.AGRICULTURE -> Color(0xFF388E3C) // Earth Green
            NoticeCategory.CITIZEN_SERVICES -> Color(0xFF0D2B4E) // Navy
            else -> Color(0xFF0D2B4E)
        }
    }

    fun getNoticeIcon(iconType: NoticeIconType): ImageVector {
        return when (iconType) {
            NoticeIconType.CIVIC -> Icons.Default.AccountBalance
            NoticeIconType.ENVIRONMENT -> Icons.Default.Air
            NoticeIconType.WEATHER -> Icons.Default.Cloud
            NoticeIconType.DISASTER -> Icons.Default.Warning
            NoticeIconType.TRANSPORT -> Icons.Default.Train
            NoticeIconType.CONSUMER -> Icons.Default.ShoppingCart
            NoticeIconType.CYBER_SAFETY -> Icons.Default.Security
            NoticeIconType.HEALTH -> Icons.Default.MedicalServices
            NoticeIconType.EDUCATION -> Icons.Default.School
            NoticeIconType.AGRICULTURE -> Icons.Default.Agriculture
            NoticeIconType.CITIZEN_SERVICES -> Icons.Default.Description
            else -> Icons.Default.Info
        }
    }
}
