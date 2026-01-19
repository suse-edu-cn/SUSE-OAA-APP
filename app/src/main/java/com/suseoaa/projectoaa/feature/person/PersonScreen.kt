package com.suseoaa.projectoaa.feature.person

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.RemeasureToBounds
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.suseoaa.projectoaa.core.util.AcademicSharedTransitionSpec

private val HeaderHeight = 320.dp

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonScreen(
    viewModel: PersonViewModel = hiltViewModel(),
    updateViewModel: AppUpdateViewModel = hiltViewModel(),
    onNavigateToLogin: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    // 【关键修复】这里使用 personInfo 对应 ViewModel 中的变量名
    val userInfo by viewModel.personInfo.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val uiEvent by viewModel.uiEvent.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val windowSizeClass = LocalWindowSizeClass.current
    val isPhone = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
    val gridColumns = if (isPhone) 1 else 2
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val scrollState = rememberLazyGridState()
    val density = LocalDensity.current
    val headerHeightPx = with(density) { HeaderHeight.toPx() }

    // 图片选择器
    val singlePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.uploadAvatar(uri)
            }
        }
    )

    // 全局 Toast
    LaunchedEffect(uiEvent) {
        uiEvent?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearUiEvent()
        }
    }

    LaunchedEffect(Unit) {
        updateViewModel.checkUpdate(isManual = false)
    }
    UpdateDialog(
        viewModel = updateViewModel,
        onDismiss = { updateViewModel.showDialog = false }
    )

    with(sharedTransitionScope) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize()) {
                // 底层：动态背景
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(HeaderHeight)
                        .graphicsLayer {
                            val scrollOffset = scrollState.firstVisibleItemScrollOffset
                            val firstVisibleIndex = scrollState.firstVisibleItemIndex

                            if (firstVisibleIndex == 0) {
                                translationY = -scrollOffset * 0.5f
                                val scale = 1f - (scrollOffset / headerHeightPx) * 0.1f
                                scaleX = scale.coerceAtLeast(0.9f)
                                scaleY = scale.coerceAtLeast(0.9f)
                                alpha =
                                    (1f - (scrollOffset / headerHeightPx) * 1.5f).coerceIn(0f, 1f)
                            } else {
                                alpha = 0f
                            }
                        }
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF9BDCE5),
                                    Color(0xFF8EC5FC),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
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

                // 顶层：滚动内容
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridColumns),
                        state = scrollState,
                        contentPadding = PaddingValues(
                            top = 16.dp + statusBarHeight,
                            bottom = 16.dp + navBarHeight + 80.dp,
                            start = 16.dp,
                            end = 16.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Spacer(modifier = Modifier.height(HeaderHeight - 80.dp))
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            UserInfoCard(
                                userInfo = userInfo,
                                onLogout = {
                                    viewModel.logout {
                                        onNavigateToLogin()
                                    }
                                },
                                onAvatarClick = {
                                    singlePhotoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                viewModel = viewModel
                            )
                        }

                        // 修改密码卡片
                        item(span = { GridItemSpan(if (isPhone) maxLineSpan else 1) }) {
                            Box(
                                modifier = Modifier.sharedBounds(
                                    sharedContentState = rememberSharedContentState(key = "change_password_card"),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    boundsTransform = AcademicSharedTransitionSpec,
                                    resizeMode = RemeasureToBounds,
                                    zIndexInOverlay = 1f
                                )
                            ) {
                                ChangePasswordCard(onClick = onNavigateToChangePassword)
                            }
                        }

                        item(span = { GridItemSpan(if (isPhone) maxLineSpan else 1) }) {
                            UpdateInfoCard(
                                currentVersion = BuildConfig.VERSION_NAME,
                                hasNewVersion = updateViewModel.hasNewVersion,
                                newVersionName = updateViewModel.latestVersionName,
                                onClick = {
                                    updateViewModel.checkUpdate(isManual = true)
                                }
                            )
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserInfoCard(
    userInfo: Data?,
    onLogout: () -> Unit,
    onAvatarClick: () -> Unit,
    viewModel: PersonViewModel
) {
    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog && userInfo != null) {
        EditInfoDialog(
            initialUsername = userInfo.username,
            initialName = userInfo.name,
            onDismiss = { showEditDialog = false },
            onConfirm = { username, name ->
                viewModel.updateInfo(username, name)
                showEditDialog = false
            }
        )
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像区域 (支持点击)
            Box(
                modifier = Modifier.clickable { onAvatarClick() }
            ) {
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
                // 编辑图标提示
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.BottomEnd)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        .padding(4.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 文本区域 (点击修改信息)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showEditDialog = true }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = userInfo?.name ?: "请登录",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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

            IconButton(onClick = onLogout) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "退出登录",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun EditInfoDialog(
    initialUsername: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var username by remember { mutableStateOf(initialUsername) }
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改个人信息") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("姓名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("用户名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(username, name) },
                enabled = username.isNotBlank() && name.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun ChangePasswordCard(onClick: () -> Unit) {
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "修改密码",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.outline
            )
        }
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
            Text(
                text = "版本信息",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (hasNewVersion) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color.Red, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "新版本: $newVersionName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "v$currentVersion",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
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
                        val release = viewModel.updateInfo!!
                        Text(
                            text = "版本: ${release.tagName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "更新内容:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .heightIn(max = 240.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            OaaMarkdownText(
                                markdown = release.body,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Button(onClick = { viewModel.downloadAndInstall() }) {
                        Text("立即下载并安装")
                    }
                } else if (!viewModel.checkStatus.contains("正在检查")) {
                    TextButton(onClick = onDismiss) {
                        Text("确定")
                    }
                }
            },
            dismissButton = {
                if (viewModel.updateInfo != null) {
                    TextButton(onClick = onDismiss) {
                        Text("稍后")
                    }
                }
            }
        )
    }
}