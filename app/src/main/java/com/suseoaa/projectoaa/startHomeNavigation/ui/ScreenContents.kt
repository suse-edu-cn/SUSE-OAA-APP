package com.suseoaa.projectoaa.startHomeNavigation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.suseoaa.projectoaa.courseList.ui.screen.CourseListScreen
import com.suseoaa.projectoaa.courseList.viewmodel.CourseListViewModel
import com.suseoaa.projectoaa.startHomeNavigation.viewmodel.ShareViewModel
import com.suseoaa.projectoaa.student.ui.StudentAppMainEntry

// ========== 页面内容实现 ==========

@Composable
fun HomeContent(viewModel: ShareViewModel) {
    val isVisible by viewModel.showOfState.collectAsState()
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { viewModel.toggleOfUI() }) {
                Text(if (viewModel.showOfState.collectAsState().value) "隐藏" else "显示")
            }
        }
        if (isVisible) {
            // 【辅助内容区域】占 40% 宽度（如：详情面板、预览等）
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(300.dp)
                    .shadow(
                        elevation = 10.dp,
                        ambientColor = Color.Gray,
                        spotColor = Color.DarkGray
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "辅助内容区", style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "这里可以显示详情、预览等内容",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SearchContent(viewModel: ShareViewModel) {
    val hint by viewModel.searchHint.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
//        显示课程列表
        val vm: CourseListViewModel = viewModel() // 或使用自定义 factory
        CourseListScreen(viewModel = vm)
    }
}

@Composable
fun SettingsContent(viewModel: ShareViewModel) {
    val info by viewModel.settingsInfo.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Settings,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.tertiary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("设置页面", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(info, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ProfileContent(viewModel: ShareViewModel) {
    val info by viewModel.profileInfo.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
//        招新换届
        StudentAppMainEntry()
    }
}