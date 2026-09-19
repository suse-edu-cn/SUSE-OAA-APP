package com.suseoaa.projectoaa.ui.screen.teachingplan.academicstatus

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.suseoaa.projectoaa.shared.domain.model.teachingplan.*

// 修读状态与课程类别对应的配色和图标。

/**
 * 获取状态颜色
 */
@Composable
internal fun getStatusColor(status: String): Color {
    return when (status) {
        StudyStatusUtils.PASSED -> Color(0xFF4CAF50)      // 绿色
        StudyStatusUtils.FAILED -> Color(0xFFE53935)      // 红色
        StudyStatusUtils.STUDYING -> Color(0xFF2196F3)    // 蓝色
        StudyStatusUtils.NOT_STUDIED -> Color(0xFF9E9E9E) // 灰色
        else -> MaterialTheme.colorScheme.onSurface
    }
}

/**
 * 获取类别颜色
 */
@Composable
internal fun getCategoryColor(categoryName: String): Color {
    return when {
        categoryName.contains("必修") -> MaterialTheme.colorScheme.primary
        categoryName.contains("选修") -> MaterialTheme.colorScheme.tertiary
        categoryName.contains("实践") -> MaterialTheme.colorScheme.secondary
        categoryName.contains("通识") -> Color(0xFF9C27B0)
        else -> MaterialTheme.colorScheme.primary
    }
}

/**
 * 获取类别图标
 */
internal fun getCategoryIcon(categoryName: String) = when {
    categoryName.contains("必修") -> Icons.Default.Star
    categoryName.contains("选修") -> Icons.Default.Menu
    categoryName.contains("实践") -> Icons.Default.Build
    categoryName.contains("通识") -> Icons.Default.Info
    categoryName.contains("核心") -> Icons.Default.Star
    else -> Icons.Default.CheckCircle
}
