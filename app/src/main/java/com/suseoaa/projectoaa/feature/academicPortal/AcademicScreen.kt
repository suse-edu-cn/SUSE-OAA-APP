package com.suseoaa.projectoaa.feature.academicPortal

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.suseoaa.projectoaa.feature.academicPortal.getGrades.GradesScreen

@Composable
fun AcademicScreen(
    isTablet: Boolean,
    onNavigate: (AcademicPortalEvent) -> Unit
) {
    // 1. [平板状态] 直接使用统一的 AcademicDestinations，默认是 Menu
    var currentDest by remember { mutableStateOf<AcademicDestinations>(AcademicDestinations.Menu) }

    // 2. [平板返回]
    BackHandler(enabled = isTablet && currentDest != AcademicDestinations.Menu) {
        currentDest = AcademicDestinations.Menu
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 3. [平板内容渲染] 使用 when 遍历 Sealed Class
        if (isTablet && currentDest != AcademicDestinations.Menu) {
            when (currentDest) {
                is AcademicDestinations.Grades -> {
                    GradesScreen(onBack = { currentDest = AcademicDestinations.Menu })
                }
                is AcademicDestinations.Test -> {}
                // 未来新增页面：
                // is AcademicDestinations.Exams -> ExamsScreen(...)
                else -> { /* 兜底 */ }
            }
        } else {
            // 4. [菜单显示]
            AcademicMenuContent(
                onItemClick = { dest ->
                    if (isTablet) {
                        // 平板：切状态
                        currentDest = dest
                    } else {
                        // 手机：发通用事件
                        onNavigate(AcademicPortalEvent.NavigateTo(dest))
                    }
                }
            )
        }
    }
}

@Composable
fun AcademicMenuContent(
    // 接收统一的对象
    onItemClick: (AcademicDestinations) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { onItemClick(AcademicDestinations.Grades) }) {
            Text("跳转到成绩页面")
        }
        Button(onClick = { onItemClick(AcademicDestinations.Test) }) {
            Text("跳转到测试页面")
        }
    }
}