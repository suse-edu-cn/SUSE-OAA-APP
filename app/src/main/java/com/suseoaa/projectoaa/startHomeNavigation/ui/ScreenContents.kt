package com.suseoaa.projectoaa.navigation.ui

// 移除了 import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.vector.ImageVector
// 移除了 import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.suseoaa.projectoaa.common.theme.OaaThemeConfig
import com.suseoaa.projectoaa.common.theme.ThemeManager
import com.suseoaa.projectoaa.navigation.viewmodel.ShareViewModel

// ==========================================
// 2. 搜索页面
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchContent(viewModel: ShareViewModel) { // 这个签名已经是正确的
    var query by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(false) }

    val currentThemeName = viewModel.currentTheme.name
    val isLegacyTheme = currentThemeName.contains("Android 4.0") || currentThemeName.contains("Android 2.3")
    val originalColorScheme = MaterialTheme.colorScheme

    val colorScheme = if (isLegacyTheme) {
        originalColorScheme.copy(
            primary = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Color.LightGray, // 用于占位符和图标
            outline = Color.Gray
        )
    } else {
        originalColorScheme
    }

    MaterialTheme(colorScheme = colorScheme) {
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
                    items(
                        items = (0..2).toList(),
                        key = { "history_item_$it" }
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
}

// ==========================================
// 3. 设置页面 (SettingsContent)
// ==========================================
@Composable
fun SettingsContent(
    // --- [修改：接收 ViewModel] ---
    viewModel: ShareViewModel,
    navController: NavHostController
) {
    // --- [修改：从 ViewModel 获取状态] ---
    val currentTheme = viewModel.currentTheme
    val notificationEnabled = viewModel.notificationEnabled

    // 移除了 context = LocalContext.current
    var showThemeDialog by remember { mutableStateOf(false) }

    val currentThemeName = currentTheme.name
    val isLegacyTheme = currentThemeName.contains("Android 4.0") || currentThemeName.contains("Android 2.3")
    val originalColorScheme = MaterialTheme.colorScheme

    val colorScheme = if (isLegacyTheme) {
        originalColorScheme.copy(
            primary = Color.White,
            onPrimary = Color.Black,
            secondary = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Color.LightGray,
            outline = Color.Gray
        )
    } else {
        originalColorScheme
    }

    MaterialTheme(colorScheme = colorScheme) {
        Box(Modifier.fillMaxSize()) {
            if (showThemeDialog) {
                ThemeSelectionDialog(
                    currentTheme = currentTheme,
                    // --- [修改：调用 ViewModel 方法] ---
                    onThemeSelected = {
                        viewModel.updateTheme(it)
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
                        SettingItem(icon = Icons.Default.Palette, title = "主题外观", subtitle = currentTheme.name, onClick = { showThemeDialog = true })
                        if (currentTheme.name.contains("二次元")) {
                            // --- [修改：调用 ViewModel 方法] ---
                            SettingItem(icon = Icons.Default.Image, title = "保存当前壁纸", subtitle = "保存到系统相册", onClick = viewModel::onSaveWallpaper)
                        }
                        SettingItem(icon = Icons.Default.Language, title = "语言", subtitle = "简体中文")
                        SettingItem(
                            icon = Icons.Default.Palette, // 或者 Icons.Default.Image
                            title = "外观与壁纸",
                            subtitle = "调整壁纸透明度",
                            onClick = { navController.navigate("settings_appearance") }
                        )
                    }
                }
                item {
                    AppCard {
                        SettingGroupTitle("账户与安全")
                        SettingItem(
                            icon = Icons.Default.Notifications,
                            title = "通知管理",
                            // --- [修改：使用 ViewModel 状态] ---
                            subtitle = if (notificationEnabled) "已开启" else "已关闭",
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
    }
}


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