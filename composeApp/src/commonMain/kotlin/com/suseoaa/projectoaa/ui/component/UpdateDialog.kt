package com.suseoaa.projectoaa.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.presentation.update.AppUpdateUiState
import com.suseoaa.projectoaa.presentation.update.AppUpdateViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * 更新对话框组件
 * @param isManualCheck 是否是手动检查更新（手动检查时，即使已经弹过也会显示）
 */
@Composable
fun UpdateDialog(
    viewModel: AppUpdateViewModel = koinViewModel(),
    onDismiss: () -> Unit,
    isManualCheck: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsState()
    val isIos = viewModel.isIos

    // 自动弹窗且已经弹过，则不显示
    if (!isManualCheck && uiState.hasShownAutoDialog && uiState.hasUpdate) {
        return
    }

    // 显示对话框的条件：正在检查、有更新、或手动检查无更新时显示“已是最新”
    val shouldShow =
        uiState.isChecking || uiState.hasUpdate || (isManualCheck && !uiState.isChecking && !uiState.hasUpdate)

    if (shouldShow) {
        // 如果是自动弹窗，标记已显示
        LaunchedEffect(uiState.hasUpdate) {
            if (uiState.hasUpdate && !isManualCheck) {
                viewModel.markDialogShown()
            }
        }

        AlertDialog(
            containerColor = MaterialTheme.colorScheme.background,
            onDismissRequest = {
                if (!uiState.isChecking) {
                    viewModel.dismissUpdateDialog()
                    onDismiss()
                }
            },
            title = {
                Text(
                    text = when {
                        uiState.latestRelease != null -> "发现新版本"
                        uiState.isChecking -> "检查更新"
                        else -> "检查更新"
                    },
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    when {
                        uiState.latestRelease != null -> {
                            val release = uiState.latestRelease!!
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
                                Text(
                                    text = release.body,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // iOS 显示提示信息
                            if (isIos) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "iOS 版本请前往 App Store 或 TestFlight 更新",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            // Android 显示下载进度
                            if (!isIos && uiState.isDownloading) {
                                Spacer(modifier = Modifier.height(16.dp))
                                LinearProgressIndicator(
                                    progress = { uiState.downloadProgress / 100f },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "正在下载... ${uiState.downloadProgress}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        uiState.isChecking -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "正在检查更新...",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }

                        uiState.errorMessage != null -> {
                            Text(
                                text = uiState.errorMessage!!,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        // 手动检查无更新时显示
                        isManualCheck -> {
                            Text(
                                text = "当前已是最新版本",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                when {
                    // iOS: 只显示知道了按钮
                    isIos && uiState.latestRelease != null -> {
                        TextButton(onClick = {
                            viewModel.dismissUpdateDialog()
                            onDismiss()
                        }) {
                            Text("知道了")
                        }
                    }
                    // Android: 显示下载按钮
                    !isIos && uiState.latestRelease != null && !uiState.isDownloading -> {
                        Button(onClick = { viewModel.startDownload() }) {
                            Text("立即下载并安装")
                        }
                    }
                    // Android: 下载完成显示安装按钮
                    !isIos && uiState.isDownloading && uiState.downloadProgress == 100 -> {
                        Button(onClick = { viewModel.installDownloadedApk() }) {
                            Text("安装更新")
                        }
                    }
                    // 无更新或检查完成时显示确定按钮
                    !uiState.isChecking && uiState.latestRelease == null -> {
                        TextButton(onClick = {
                            viewModel.dismissUpdateDialog()
                            onDismiss()
                        }) {
                            Text("确定")
                        }
                    }
                }
            },
            dismissButton = {
                // Android 且有更新时显示稍后按钮
                if (!isIos && uiState.latestRelease != null && !uiState.isDownloading) {
                    TextButton(onClick = {
                        viewModel.dismissUpdateDialog()
                        onDismiss()
                    }) {
                        Text("稍后")
                    }
                }
            }
        )
    }
}

/**
 * 手动检查更新按钮卡片
 */
@Composable
fun CheckUpdateCard(
    viewModel: AppUpdateViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        UpdateDialog(
            viewModel = viewModel,
            onDismiss = { showDialog = false },
            isManualCheck = true
        )
    }

    Card(
        onClick = {
            showDialog = true
            viewModel.checkForUpdate()
        },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "检查更新",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (uiState.isChecking) "正在检查..." else "点击检查是否有新版本",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (uiState.isChecking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}
