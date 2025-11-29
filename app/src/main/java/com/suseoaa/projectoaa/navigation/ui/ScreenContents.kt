package com.suseoaa.projectoaa.navigation.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.suseoaa.projectoaa.common.theme.OaaThemeConfig
import com.suseoaa.projectoaa.common.theme.ThemeManager
import com.suseoaa.projectoaa.navigation.viewmodel.ShareViewModel

// ==========================================
// 1. 首页内容 (HomeContent)
// ==========================================
@Composable
fun HomeContent(
    currentThemeName: String,
    onRefreshWallpaper: (Context) -> Unit,
    onSaveWallpaper: (Context) -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "欢迎回来，Project OAA", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(text = "今天也是充满活力的一天！", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (currentThemeName.contains("二次元")) {
                    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                        IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "更多选项", tint = MaterialTheme.colorScheme.primary) }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), RoundedCornerShape(12.dp))) {
                            DropdownMenuItem(
                                text = { Text("刷新壁纸") },
                                onClick = {
                                    onRefreshWallpaper(context)
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Refresh, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("保存壁纸") },
                                onClick = {
                                    onSaveWallpaper(context)
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Download, null) }
                            )
                        }
                    }
                }
            }
        }

        item {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Campaign, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("协会公告", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text("🎉 2025年春季招新活动即将开始，请各位干事做好准备！", style = MaterialTheme.typography.bodyMedium)
            }
        }

        item {
            Text("待办事项", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp))
        }

        // 为 LazyColumn 添加 key，提高滚动性能
        items(
            items = (0..4).toList(), // 假设是5个
            key = { "task_item_$it" } // 提供一个稳定的 Key
        ) { index ->
            TaskItem(index = index)
        }

        item { Spacer(modifier = Modifier.height(60.dp)) }
    }
}

@Composable
private fun TaskItem(index: Int) {
    AppCard {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Assignment, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text("协会事务处理事项 #${index + 1}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("截止日期: 2025-12-31", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

// ==========================================
// 2. 搜索页面 (SearchContent)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchContent(viewModel: ShareViewModel) {
    var query by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            query = query, onQueryChange = { query = it }, onSearch = { active = false }, active = active, onActiveChange = { active = it },
            placeholder = { Text("搜索...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = { if (active) { IconButton(onClick = { if (query.isNotEmpty()) query = "" else active = false }) { Icon(Icons.Default.Close, null) } } },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = SearchBarDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f),
                dividerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                inputFieldColors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f), unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f))
            )
        ) {
            LazyColumn {
                // [优化] 为 LazyColumn 添加 key
                items(
                    items = (0..2).toList(), // 假设是3个历史记录
                    key = { "history_item_$it" } // 提供稳定的 Key
                ) {
                    ListItem(
                        headlineContent = { Text("历史记录: 招新面试表 $it") },
                        leadingContent = { Icon(Icons.Default.History, null) },
                        modifier = Modifier.clickable { query = "招新面试表 $it"; active = false },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f))
                    )
                }
            }
        }
        Box(Modifier.fillMaxSize(), Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Search, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.surfaceVariant); Spacer(Modifier.height(16.dp)); Text("输入关键词开始搜索", color = MaterialTheme.colorScheme.outline) } }
    }
}

// ==========================================
// 3. 设置页面 (SettingsContent)
// ==========================================
@Composable
fun SettingsContent(
    currentTheme: OaaThemeConfig,
    notificationEnabled: Boolean,
    onThemeSelected: (OaaThemeConfig) -> Unit,
    onSaveWallpaper: (Context) -> Unit,
    navController: NavHostController
) {
    val context = LocalContext.current
    var showThemeDialog by remember { mutableStateOf(false) }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = currentTheme,
            onThemeSelected = {
                onThemeSelected(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text("设置", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)) }

        item {
            AppCard {
                SettingGroupTitle("通用")
                SettingItem(icon = Icons.Default.Palette, title = "主题外观", subtitle = currentTheme.name, onClick = { showThemeDialog = true }) // [修改]
                if (currentTheme.name.contains("二次元")) {
                    SettingItem(icon = Icons.Default.Image, title = "保存当前壁纸", subtitle = "保存到系统相册", onClick = { onSaveWallpaper(context) }) // [修改]
                }
                SettingItem(icon = Icons.Default.Language, title = "语言", subtitle = "简体中文")
            }
        }

        item {
            AppCard {
                SettingGroupTitle("账户与安全")
                SettingItem(
                    icon = Icons.Default.Notifications,
                    title = "通知管理",
                    subtitle = if (notificationEnabled) "已开启" else "已关闭", // [修改]
                    onClick = { navController.navigate("settings_notifications") }
                )
                SettingItem(
                    icon = Icons.Default.Security,
                    title = "隐私设置",
                    onClick = { navController.navigate("settings_privacy") }
                )
            }
        }

        item {
            AppCard {
                SettingGroupTitle("关于")
                SettingItem(icon = Icons.Default.Info, title = "关于 Project OAA", subtitle = "版本 v1.0.0 Alpha", onClick = { navController.navigate("settings_about") })
                SettingItem(icon = Icons.Default.BugReport, title = "反馈问题", onClick = { navController.navigate("settings_feedback") })
            }
        }

        item { Spacer(modifier = Modifier.height(60.dp)) }
    }
}

// === 辅助组件 ===
@Composable private fun ThemeSelectionDialog(currentTheme: OaaThemeConfig, onThemeSelected: (OaaThemeConfig) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("选择主题") }, text = { Column { ThemeManager.themeList.forEach { theme -> ThemeOption(text = theme.name, selected = currentTheme.name == theme.name, onClick = { onThemeSelected(theme) }) } } }, confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } }, containerColor = MaterialTheme.colorScheme.surface)
}
@Composable private fun ThemeOption(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 0.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = selected, onClick = onClick); Spacer(Modifier.width(8.dp)); Text(text) }
}
@Composable private fun SettingGroupTitle(title: String) {
    Text(text = title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, top = 0.dp, end = 0.dp, bottom = 8.dp) )
}
@Composable private fun SettingItem(icon: ImageVector, title: String, subtitle: String? = null, onClick: () -> Unit = {}) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 0.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(16.dp)); Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface); if (subtitle != null) { Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } }; Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline) }
}