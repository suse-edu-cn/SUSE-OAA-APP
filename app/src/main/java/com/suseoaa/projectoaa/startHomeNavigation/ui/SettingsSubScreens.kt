package com.suseoaa.projectoaa.navigation.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suseoaa.projectoaa.navigation.viewmodel.ShareViewModel
import kotlin.math.roundToInt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

// ==========================================
// 4.1 通知管理界面
// ==========================================
@Composable
fun NotificationsScreen(
    viewModel: ShareViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasSystemPermission by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasSystemPermission = NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    fun openSystemNotificationSettings(ctx: Context) {
        val intent = Intent().apply {
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
        }
        ctx.startActivity(intent)
    }

    SettingsSubScreenScaffold(title = "通知管理", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppCard {
                Column(Modifier.padding(horizontal = 8.dp, vertical = 12.dp)) {
                    Text("系统推送通知", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "管理应用是否可以向您发送推送通知。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("当前状态:", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (hasSystemPermission) "已开启" else "已关闭",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (hasSystemPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { openSystemNotificationSettings(context) }, modifier = Modifier.fillMaxWidth()) {
                        Text("前往系统设置管理")
                    }
                }
            }
            AppCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("应用内提醒", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "例如首页的公告横幅（非系统推送）",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = viewModel.notificationEnabled,
                        onCheckedChange = viewModel::onNotificationToggleChanged
                    )
                }
            }
        }
    }
}

// ==========================================
// 4.2 隐私设置界面
// ==========================================
@Composable
fun PrivacyScreen(
    viewModel: ShareViewModel,
    onBack: () -> Unit
) {
    SettingsSubScreenScaffold(title = "隐私设置", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            AppCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("个性化推荐", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "允许我们使用您的数据",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = viewModel.privacyEnabled,
                        onCheckedChange = viewModel::onPrivacyToggleChanged
                    )
                }
            }
        }
    }
}

// ==========================================
// 4.3 关于界面
// ==========================================
@Composable
fun AboutScreen(onBack: () -> Unit) {
    SettingsSubScreenScaffold(title = "关于 Project OAA", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Icon(
                Icons.Default.Info,
                null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Project OAA",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Version 1.0.0 Alpha",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            AppCard {
                Text(
                    "本项目旨在提供一个高效、现代化的协会管理工具，" +
                            "基于最新的技术栈构建，包括 Kotlin, Jetpack Compose, Ktor 和 MVVM 架构。" +
                            "感谢所有为此项目贡献的开发者。",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

// ==========================================
// 4.4 反馈问题界面
// ==========================================
@Composable
fun FeedbackScreen(
    viewModel: ShareViewModel,
    onBack: () -> Unit
) {
    SettingsSubScreenScaffold(title = "反馈问题", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "请描述您遇到的问题或建议：",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = viewModel.feedbackText,
                    onValueChange = viewModel::onFeedbackTextChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    placeholder = { Text("详细描述有助于我们更快解决问题...") },
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }

            Button(
                onClick = viewModel::submitFeedback,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 8.dp),
                enabled = !viewModel.isSubmittingFeedback,
                shape = RoundedCornerShape(16.dp)
            ) {
                if (viewModel.isSubmittingFeedback) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("提交反馈", fontSize = 18.sp)
                }
            }
        }
    }
}

// ==========================================
// 4.5 外观设置界面 (新增：壁纸管理)
// ==========================================
@Composable
fun AppearanceScreen(
    viewModel: ShareViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    // 监听壁纸遮罩透明度
    val currentAlpha by viewModel.wallpaperAlpha.collectAsStateWithLifecycle()
    // 检查当前是否为二次元主题
    val isAnimeTheme = viewModel.currentTheme.name.contains("二次元")

    SettingsSubScreenScaffold(title = "外观设置", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isAnimeTheme) {
                // 1. 壁纸透明度控制
                AppCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text("壁纸遮罩浓度", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "调整背景壁纸的变暗程度，浓度越高，文字越清晰。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // 滑块
                        Slider(
                            value = currentAlpha,
                            onValueChange = { newValue ->
                                viewModel.updateWallpaperAlpha( newValue)
                            },
                            valueRange = 0f..1f,
                            steps = 38
                        )

                        // 底部文字指示
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("透亮", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "${(currentAlpha * 100).roundToInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text("纯色", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // 2. 手动刷新壁纸 (作为首页右上角的备用入口)
                AppCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("手动刷新壁纸", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "立即从网络下载并切换一张新的随机壁纸。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(onClick = { viewModel.onRefreshWallpaper() }) {
                            Text("刷新")
                        }
                    }
                }
            } else {
                // 非二次元主题的提示
                AppCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "当前主题不支持自定义壁纸",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "请在“设置 - 主题”中切换到“二次元主题”以启用壁纸设置功能。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}