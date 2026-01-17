package com.suseoaa.projectoaa.feature.person

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.material3.TimePickerDialogDefaults.Title
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.suseoaa.projectoaa.BuildConfig
import com.suseoaa.projectoaa.app.LocalWindowSizeClass
import com.suseoaa.projectoaa.core.network.model.person.Data
import com.suseoaa.projectoaa.core.ui.component.OaaMarkdownText

// 定义头图高度
private val HeaderHeight = 320.dp

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonScreen(
    viewModel: PersonViewModel = hiltViewModel(),
    updateViewModel: AppUpdateViewModel = hiltViewModel(),
    onNavigateToLogin: () -> Unit
) {
    val userInfo by viewModel.userInfo.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.errorMessage.collectAsStateWithLifecycle()

    val windowSizeClass = LocalWindowSizeClass.current
    // 手机端单列(看起来像列表)，平板端双列或多列
    val isPhone = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
    val gridColumns = if (isPhone) 1 else 2
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // 1. 滚动状态用于驱动动画
    val scrollState = rememberLazyGridState()
    val density = LocalDensity.current
    val headerHeightPx = with(density) { HeaderHeight.toPx() }

//    自动检查更新
    LaunchedEffect(Unit) {
        updateViewModel.checkUpdate(isManual = false)
    }
    UpdateDialog(
        viewModel = updateViewModel,
        onDismiss = { updateViewModel.showDialog = false }
    )
    Scaffold(
        // 将背景设为透明，以便看到我们自定义的底层
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // 2. 底层：动态背景
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HeaderHeight)
                    .graphicsLayer {
                        // 计算滚动偏移量
                        val scrollOffset = scrollState.firstVisibleItemScrollOffset
                        val firstVisibleIndex = scrollState.firstVisibleItemIndex

                        // 只有当第一个 Item (Spacer) 可见时才应用视差
                        if (firstVisibleIndex == 0) {
                            // Parallax: 背景移动速度是前景的一半 (0.5f)
                            translationY = -scrollOffset * 0.5f
                            // Scale: 稍微缩小，模拟“下沉”效果
                            val scale = 1f - (scrollOffset / headerHeightPx) * 0.1f
                            scaleX = scale.coerceAtLeast(0.9f)
                            scaleY = scale.coerceAtLeast(0.9f)
                            // Alpha: 慢慢变淡
                            alpha = (1f - (scrollOffset / headerHeightPx) * 1.5f).coerceIn(0f, 1f)
                        } else {
                            // 如果滑得太远，直接隐藏背景避免遮挡
                            alpha = 0f
                        }
                    }
                    .background(
                        // 模仿 HyperOS 的渐变色
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF9BDCE5),
                                Color(0xFF8EC5FC),
                                MaterialTheme.colorScheme.background // 底部渐变融合
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // 背景中的大标题
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "青蟹",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "致力服务于四川轻化工大学开放原子开源协会",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black.copy(alpha = 0.5f)
                    )
                }
            }

            // 3. 顶层：滚动内容
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    state = scrollState,
                    contentPadding = PaddingValues(
                        top = 16.dp + statusBarHeight, // 让内容避开状态栏
                        bottom = 16.dp + navBarHeight + 80.dp, // 底部避开导航栏和 BottomBar
                        start = 16.dp,
                        end = 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Item 0: 透明占位符，高度等于头图，让背景露出来
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Spacer(modifier = Modifier.height(HeaderHeight - 80.dp)) // 稍微留一点重叠
                    }

                    // Item 1: 用户信息卡片 (跨满整行)
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        UserInfoCard(
                            userInfo = userInfo,
                            onLogout = {
                                viewModel.logout {
                                    onNavigateToLogin()
                                }
                            }
                        )
                    }
                    item(span = { GridItemSpan(if (isPhone) maxLineSpan else 1) }) {
                        UpdateInfoCard(
                            currentVersion = BuildConfig.VERSION_NAME,
                            hasNewVersion = updateViewModel.hasNewVersion,
                            newVersionName = updateViewModel.latestVersionName,
                            onClick = {
                                // 点击触发手动检查
                                updateViewModel.checkUpdate(isManual = true)
                            }
                        )
                    }
                    // 底部留白
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

// 组件封装
@Composable
fun UserInfoCard(
    userInfo: Data?,
    onLogout: () -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(), // 确保 Row 占满宽度
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. 头像
            AsyncImage(
                model = userInfo?.avatar ?: "",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // 2. 文本区域
            // 使用 weight(1f) 占据中间剩余空间，把后面的按钮挤到最右边
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userInfo?.name ?: "请登录",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = userInfo?.department ?: "暂未加入任何部门",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = userInfo?.role ?: "查看会员权益",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // --- 3. 右侧退出按钮 ---
            // 只有当用户已登录(有名字/信息)时才显示，或者一直显示
            IconButton(onClick = onLogout) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "退出登录",
                    tint = MaterialTheme.colorScheme.error // 使用红色表示警告/退出
                )
            }
        }
    }
}

// 退出登录卡片组件
@Composable
fun LogoutCard(onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error // 使用红色示警
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "退出登录",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun UpdateDialog(
    viewModel: AppUpdateViewModel,
    onDismiss: () -> Unit
) {
    if (viewModel.showDialog) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.background,
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = if (viewModel.updateInfo != null) "发现新版本" else "检查更新",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    if (viewModel.updateInfo != null) {
                        // === 场景 A: 发现新版本，显示详情 ===
                        val release = viewModel.updateInfo!!

                        // 1. 版本号
                        Text(
                            text = "版本: ${release.tagName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 2. 更新日志标题
                        Text(
                            text = "更新内容:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 3. Markdown 内容区域 (带滚动条)
                        Box(
                            modifier = Modifier
                                .heightIn(max = 240.dp) // 限制最大高度，防止弹窗太长
                                .verticalScroll(rememberScrollState())
                        ) {
                            // 使用通用的 Markdown 解析组件
                            OaaMarkdownText(
                                markdown = release.body,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // 场景 B: 正在检查 或 暂无更新
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // 如果状态包含 "检查" 关键字，显示转圈圈
                            if (viewModel.checkStatus.contains("检查") && !viewModel.checkStatus.contains(
                                    "失败"
                                )
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                            // 显示状态文字 (如 "正在检查..." 或 "当前已是最新版本")
                            Text(
                                text = viewModel.checkStatus,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (viewModel.updateInfo != null) {
                    // 有更新 -> 显示“立即更新”
                    Button(onClick = { viewModel.downloadAndInstall() }) {
                        Text("立即下载并安装")
                    }
                } else if (!viewModel.checkStatus.contains("正在检查")) {
                    // 没更新且检查结束 -> 显示“确定”关闭弹窗
                    TextButton(onClick = onDismiss) {
                        Text("确定")
                    }
                }
            },
            dismissButton = {
                // 只有在显示更新详情时，才显示“稍后”按钮
                if (viewModel.updateInfo != null) {
                    TextButton(onClick = onDismiss) {
                        Text("稍后")
                    }
                }
            }
        )
    }
}


@Composable
fun UpdateInfoCard(
    currentVersion: String,
    hasNewVersion: Boolean,
    newVersionName: String,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左侧标题
            Text(
                text = "版本信息",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            // 右侧信息区域
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (hasNewVersion) {
                    // 有新版本时的样式

                    // 1. 红点
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color.Red, CircleShape)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // 2. 新版本号提示 (例如: "v1.0.0 -> v1.1.0")
                    Text(
                        text = "新版本: $newVersionName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary, // 用高亮色
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    // 无新版本时的样式
                    Text(
                        text = "v$currentVersion",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 箭头图标
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}