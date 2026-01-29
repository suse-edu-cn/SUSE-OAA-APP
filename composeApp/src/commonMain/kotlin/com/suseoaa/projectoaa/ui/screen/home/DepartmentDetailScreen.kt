package com.suseoaa.projectoaa.ui.screen.home

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.suseoaa.projectoaa.presentation.home.HomeViewModel
import com.suseoaa.projectoaa.ui.component.OaaMarkdownText
import com.suseoaa.projectoaa.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * 部门详情页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartmentDetailScreen(
    departmentName: String,
    onBack: () -> Unit,
    viewModel: HomeViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 加载详情
    LaunchedEffect(departmentName) {
        viewModel.fetchDetailInfo(departmentName)
    }

    // 全屏编辑弹窗
    if (uiState.showEditDialog) {
        EditAnnouncementDialog(
            departmentName = departmentName,
            content = uiState.editContent,
            isUpdating = uiState.isUpdating,
            onContentChange = viewModel::onEditContentChange,
            onSave = viewModel::submitUpdate,
            onDismiss = { viewModel.toggleEditDialog(false) }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            departmentName, 
                            fontWeight = FontWeight.Bold
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = uiState.canEditCurrent && uiState.detailData != null,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    FloatingActionButton(
                        onClick = { viewModel.toggleEditDialog(true) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.Edit, "编辑")
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.TopStart
            ) {
                when {
                    // 加载中
                    uiState.isLoadingDetail && !uiState.isUpdating -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    // 错误
                    uiState.detailError != null -> {
                        ErrorContent(
                            error = uiState.detailError ?: "未知错误",
                            onRetry = { viewModel.fetchDetailInfo(departmentName) },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    // 显示内容
                    uiState.detailData != null -> {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                top = 16.dp,
                                bottom = 88.dp
                            )
                        ) {
                            item {
                                OaaMarkdownText(
                                    markdown = uiState.detailData!!.data,
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        lineHeight = 28.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 错误内容
 */
@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Text(
            "加载失败", 
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            error, 
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onRetry) {
            Text("重试")
        }
    }
}

/**
 * 编辑公告对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditAnnouncementDialog(
    departmentName: String,
    content: String,
    isUpdating: Boolean,
    onContentChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text("编辑${departmentName}介绍") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, "关闭")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = onSave,
                            enabled = !isUpdating
                        ) {
                            Text(
                                "保存", 
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                if (isUpdating) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                TextField(
                    value = content,
                    onValueChange = onContentChange,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    placeholder = { 
                        Text(
                            "在此输入 Markdown 内容...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ) 
                    },
                    textStyle = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
