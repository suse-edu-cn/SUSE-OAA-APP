package com.suseoaa.projectoaa.ui.screen.checkin

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

// 签到任务状态色板：待签到 / 已完成 / 缺勤，分前景与背景两套，暗色模式各有一版。

// 自定义颜色 - 适配暗色模式
@Composable
internal fun getTaskPendingColor() = if (androidx.compose.foundation.isSystemInDarkTheme()) {
    Color(0xFF90CAF9) // 浅蓝色 - 暗色模式
} else {
    Color(0xFF1976D2) // 深蓝色 - 亮色模式
}

@Composable
internal fun getTaskCompletedColor() = if (androidx.compose.foundation.isSystemInDarkTheme()) {
    Color(0xFF81C784) // 浅绿色 - 暗色模式
} else {
    Color(0xFF388E3C) // 深绿色 - 亮色模式
}

@Composable
internal fun getTaskAbsentColor() = if (androidx.compose.foundation.isSystemInDarkTheme()) {
    Color(0xFFE57373) // 浅红色 - 暗色模式
} else {
    Color(0xFFD32F2F) // 深红色 - 亮色模式
}

@Composable
internal fun getTaskPendingBgColor() = if (androidx.compose.foundation.isSystemInDarkTheme()) {
    Color(0xFF1E3A5F) // 暗蓝色背景
} else {
    Color(0xFFE3F2FD) // 浅蓝色背景
}

@Composable
internal fun getTaskCompletedBgColor() = if (androidx.compose.foundation.isSystemInDarkTheme()) {
    Color(0xFF1B5E20) // 暗绿色背景
} else {
    Color(0xFFE8F5E9) // 浅绿色背景
}

@Composable
internal fun getTaskAbsentBgColor() = if (androidx.compose.foundation.isSystemInDarkTheme()) {
    Color(0xFF5F2120) // 暗红色背景
} else {
    Color(0xFFFFEBEE) // 浅红色背景
}

// 打卡状态颜色 - 已移除，改用MaterialTheme主题颜色适配暗色模式
