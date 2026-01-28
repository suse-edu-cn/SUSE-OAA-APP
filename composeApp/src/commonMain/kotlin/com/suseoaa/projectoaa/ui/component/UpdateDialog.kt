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
 */
@Composable
fun UpdateDialog(
    viewModel: AppUpdateViewModel = koinViewModel(),
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    if (uiState.hasUpdate || uiState.isChecking) {
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
                    text = if (uiState.latestRelease != null) "发现新版本" else "检查更新",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    if (uiState.latestRelease != null) {
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
                            // 简单显示更新说明文本
                            Text(
                                text = release.body,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        if (uiState.isDownloading) {
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
                    } else if (uiState.isChecking) {
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
                    } else if (uiState.errorMessage != null) {
                        Text(
                            text = uiState.errorMessage!!,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                when {
                    uiState.latestRelease != null && !uiState.isDownloading -> {
                        Button(onClick = { viewModel.startDownload() }) {
                            Text("立即下载并安装")
                        }
                    }
                    uiState.isDownloading && uiState.downloadProgress == 100 -> {
                        Button(onClick = { viewModel.installDownloadedApk() }) {
                            Text("安装更新")
                        }
                    }
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
                if (uiState.latestRelease != null && !uiState.isDownloading) {
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
            onDismiss = { showDialog = false }
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
