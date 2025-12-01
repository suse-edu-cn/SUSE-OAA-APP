package com.suseoaa.projectoaa.navigation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.suseoaa.projectoaa.common.navigation.AppRoutes
import com.suseoaa.projectoaa.navigation.viewmodel.HomeUiState
import com.suseoaa.projectoaa.navigation.viewmodel.HomeViewModel
import com.suseoaa.projectoaa.navigation.viewmodel.ShareViewModel

// 用于传递计算好的颜色
private data class HomeDisplayColors(
    val primary: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val secondary: Color,
    val tertiary: Color,
    val outline: Color
)

// ==========================================
// 1. HomeScreen (入口 - 智能 Composable)
// ==========================================
@Composable
fun HomeScreen(
    navController: NavHostController,
    homeViewModel: HomeViewModel = hiltViewModel(),
    shareViewModel: ShareViewModel
) {
    // HomeViewModel 只包含日期和倒计时
    val uiState = homeViewModel.uiState
    val currentTheme = shareViewModel.currentTheme

    // 计算主题和颜色
    val currentThemeName = currentTheme.name
    val isAnimeTheme = currentThemeName.contains("二次元")
    val isLegacyTheme = currentThemeName.contains("Android 4.0") || currentThemeName.contains("Android 2.3")

    val colorScheme = MaterialTheme.colorScheme
    val displayColors = HomeDisplayColors(
        primary = if (isLegacyTheme) Color.White else colorScheme.primary,
        onSurface = if (isLegacyTheme) Color.White else colorScheme.onSurface,
        onSurfaceVariant = if (isLegacyTheme) Color.White.copy(alpha = 0.75f) else colorScheme.onSurfaceVariant,
        secondary = if (isLegacyTheme) Color.White else colorScheme.secondary,
        tertiary = if (isLegacyTheme) Color.White else colorScheme.tertiary,
        outline = if (isLegacyTheme) Color.Gray else colorScheme.outline
    )

    // 将状态传递给哑组件
    HomeContent(
        uiState = uiState,
        isAnimeTheme = isAnimeTheme,
        colors = displayColors,
        navController = navController,
        onRefreshWallpaper = shareViewModel::onRefreshWallpaper,
        onSaveWallpaper = shareViewModel::onSaveWallpaper
    )
}

// ==========================================
// 2. HomeContent (纯 UI - 哑 Composable)
// ==========================================
@Composable
private fun HomeContent(
    uiState: HomeUiState,
    isAnimeTheme: Boolean,
    colors: HomeDisplayColors,
    navController: NavHostController,
    onRefreshWallpaper: () -> Unit,
    onSaveWallpaper: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 头部欢迎 ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "欢迎回来，Project OAA",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                    Text(
                        text = "今天也是充满活力的一天！",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant
                    )
                }

                if (isAnimeTheme) {
                    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "更多选项", tint = colors.primary)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                RoundedCornerShape(12.dp)
                            )
                        ) {
                            DropdownMenuItem(
                                text = { Text("刷新背景") },
                                onClick = { onRefreshWallpaper(); showMenu = false },
                                leadingIcon = { Icon(Icons.Default.Refresh, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("保存背景") },
                                onClick = { onSaveWallpaper(); showMenu = false },
                                leadingIcon = { Icon(Icons.Default.Download, null) }
                            )
                        }
                    }
                }
            }
        }

        // --- 打卡卡片 ---
        item {
            // 调用外部的 CheckInCard 组件
            CheckInCard(
                homeUiState = uiState,
                primaryColor = colors.primary,
                onSurfaceColor = colors.onSurface,
                onSurfaceVariantColor = colors.onSurfaceVariant
            )
        }

        // --- 协会公告 ---
        item {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Campaign, null, tint = colors.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "协会公告",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "🎉 2025年春季招新活动即将开始，请各位干事做好准备！",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface
                )
            }
        }

        // --- 快捷功能 ---
        item {
            Text(
                "快捷功能",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
                color = colors.onSurface
            )
        }

        // 课表
        item {
            AppCard(onClick = { navController.navigate(AppRoutes.CourseList.route) }) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppRoutes.CourseList.icon, null, tint = colors.secondary, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(AppRoutes.CourseList.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                }
            }
        }

        // 招新
        item {
            AppCard(onClick = { navController.navigate(AppRoutes.StudentForm.route) }) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppRoutes.StudentForm.icon, null, tint = colors.tertiary, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(AppRoutes.StudentForm.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                }
            }
        }

        // --- 待办事项 ---
        item {
            Text("待办事项", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp), color = colors.onSurface)
        }
        items((0..2).toList(), key = { "task_$it" }) { index ->
            TaskItem(
                index = index,
                onSurfaceColor = colors.onSurface,
                secondaryColor = colors.secondary,
                outlineColor = colors.outline,
                // [修改] 添加导航点击事件
                onClick = {
                    // 我们使用 "todo_detail" 作为新路由，并将 index 作为 ID 传递
                    navController.navigate("todo_detail/$index")
                }
            )
        }
        item { Spacer(modifier = Modifier.height(60.dp)) }
    }
}

// ==========================================
// 3. 辅助组件
// ==========================================

@Composable
private fun TaskItem(
    index: Int,
    onSurfaceColor: Color,
    secondaryColor: Color,
    outlineColor: Color,
    onClick: () -> Unit
) {
    AppCard(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Assignment, null, tint = secondaryColor, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    "协会事务处理事项 #${index + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = onSurfaceColor
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "截止日期: 2025-12-31",
                    style = MaterialTheme.typography.bodySmall,
                    color = outlineColor
                )
            }
        }
    }
}