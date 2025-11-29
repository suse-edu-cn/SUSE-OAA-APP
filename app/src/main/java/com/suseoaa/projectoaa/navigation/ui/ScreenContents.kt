package com.suseoaa.projectoaa.navigation.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.common.theme.OaaThemeConfig
import com.suseoaa.projectoaa.common.theme.ThemeManager
import com.suseoaa.projectoaa.navigation.viewmodel.ShareViewModel

// ==========================================
// 公共组件：统一风格的磨砂卡片
// 使用 85% 不透明度，确保二次元主题下壁纸可见
// ==========================================
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            // [关键设置] 0.85f 透明度
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        ),
        onClick = onClick ?: {}
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

// ==========================================
// 1. 首页内容 (HomeContent)
// ==========================================
@Composable
fun HomeContent(viewModel: ShareViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(
                    text = "欢迎回来，Project OAA",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "今天也是充满活力的一天！",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "协会公告",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "🎉 2025年春季招新活动即将开始，请各位干事做好准备！",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        item {
            Text(
                "待办事项",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
            )
        }

        items(5) { index ->
            TaskItem(index = index)
        }

        // 底部留白，防止被导航栏遮挡
        item { Spacer(modifier = Modifier.height(60.dp)) }
    }
}

@Composable
private fun TaskItem(index: Int) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Assignment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "协会事务处理事项 #${index + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "截止日期: 2025-12-31",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
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
            query = query,
            onQueryChange = { query = it },
            onSearch = { active = false },
            active = active,
            onActiveChange = { active = it },
            placeholder = { Text("搜索...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (active) {
                    IconButton(onClick = {
                        if (query.isNotEmpty()) query = "" else active = false
                    }) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            // [优化] 搜索栏背景半透明
            colors = SearchBarDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            )
        ) {
            LazyColumn {
                items(3) {
                    ListItem(
                        headlineContent = { Text("历史记录: 招新面试表 $it") },
                        leadingContent = { Icon(Icons.Default.History, null) },
                        modifier = Modifier.clickable {
                            query = "招新面试表 $it"
                            active = false
                        },
                        // [优化] 列表项背景半透明
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 当内容为空时，这里也可以透过壁纸
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "输入关键词开始搜索",
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

// ==========================================
// 3. 设置页面 (SettingsContent)
// ==========================================
@Composable
fun SettingsContent(viewModel: ShareViewModel) {
    var showThemeDialog by remember { mutableStateOf(false) }

    // 主题选择弹窗
    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = viewModel.currentTheme,
            onThemeSelected = { newTheme ->
                viewModel.updateTheme(newTheme)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "设置",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )
        }

        item {
            AppCard {
                SettingGroupTitle("通用")

                // 主题设置：显示当前选中的主题名称
                SettingItem(
                    icon = Icons.Default.Palette,
                    title = "主题外观",
                    subtitle = viewModel.currentTheme.name,
                    onClick = { showThemeDialog = true }
                )

                SettingItem(icon = Icons.Default.Language, title = "语言", subtitle = "简体中文")
            }
        }

        item {
            AppCard {
                SettingGroupTitle("账户与安全")
                SettingItem(icon = Icons.Default.Notifications, title = "通知管理", subtitle = "已开启")
                SettingItem(icon = Icons.Default.Security, title = "隐私设置")
            }
        }

        item {
            AppCard {
                SettingGroupTitle("关于")
                SettingItem(icon = Icons.Default.Info, title = "关于 Project OAA", subtitle = "版本 v1.0.0 Alpha")
                SettingItem(icon = Icons.Default.BugReport, title = "反馈问题")
            }
        }

        item { Spacer(modifier = Modifier.height(60.dp)) }
    }
}

// === 辅助组件 ===

@Composable
private fun ThemeSelectionDialog(
    currentTheme: OaaThemeConfig,
    onThemeSelected: (OaaThemeConfig) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择主题") },
        text = {
            Column {
                // 动态遍历 ThemeManager 中定义的所有主题
                ThemeManager.themeList.forEach { theme ->
                    ThemeOption(
                        text = theme.name,
                        selected = currentTheme.name == theme.name,
                        onClick = { onThemeSelected(theme) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        // 确保弹窗背景是不透明的，防止与底下复杂的壁纸混杂
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun ThemeOption(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 0.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}

@Composable
private fun SettingGroupTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 0.dp, end = 0.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 0.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline
        )
    }
}