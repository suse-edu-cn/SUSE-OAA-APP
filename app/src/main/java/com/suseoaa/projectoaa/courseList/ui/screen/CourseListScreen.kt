package com.suseoaa.projectoaa.courseList.ui.screen


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suseoaa.projectoaa.courseList.viewmodel.CourseListViewModel
import com.suseoaa.projectoaa.courseList.data.remote.dto.Kb

/**
 * 课表Screen
 * MVVM架构 - View层（UI展示）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseListScreen(
    viewModel: CourseListViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val courseData by viewModel.courseData.collectAsStateWithLifecycle()
    val uiState = viewModel.uiState
    var showLoginDialog by remember { mutableStateOf(false) }

    Scaffold(
        // 保持UI风格统一
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("课表查询") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showLoginDialog = true }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "获取课表")
            }
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 状态指示器
            CourseStatusBar(
                statusMessage = uiState.statusMessage,
                successMessage = uiState.successMessage,
                errorMessage = uiState.errorMessage,
                onDismiss = { viewModel.clearMessages() }
            )

            // 加载状态
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // 课表数据显示
            courseData?.let { data ->
                CourseListContent(courses = data.kbList ?: emptyList())
            } ?: run {
                // 移除空状态下的按钮，统一使用右下角 FAB
                EmptyCourseState()
            }
        }
    }

    // 登录对话框
    if (showLoginDialog) {
        LoginDialog(
            onDismiss = { showLoginDialog = false },
            onConfirm = { username, password ->
                viewModel.fetchAndSaveCourseSchedule(username, password)
                showLoginDialog = false
            }
        )
    }
}

@Composable
private fun CourseStatusBar(
    statusMessage: String?,
    successMessage: String?,
    errorMessage: String?,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        statusMessage?.let {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        successMessage?.let {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        errorMessage?.let {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CourseListContent(courses: List<Kb?>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(courses.filterNotNull(),key = { course -> course.kch ?: course.kcmc ?: "" }) { course ->
            CourseCard(course = course)
        }
    }
}

@Composable
private fun CourseCard(course: Kb) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = course.kcmc ?: "未知课程",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            CourseDetailRow(
                icon = Icons.Default.Person,
                label = "教师",
                value = course.xm ?: "未知"
            )
            
            CourseDetailRow(
                icon = Icons.Default.Place,
                label = "地点",
                value = course.cdmc ?: "未知"
            )
            
            CourseDetailRow(
                icon = Icons.Default.DateRange,
                label = "时间",
                value = "${course.xqjmc ?: "未知"} ${course.jc ?: ""}"
            )
            
            CourseDetailRow(
                icon = Icons.Default.Info,
                label = "周次",
                value = course.zcd ?: "未知"
            )
            
            course.kcxz?.let { nature ->
                Spacer(modifier = Modifier.height(8.dp))
                AssistChip(
                    onClick = { },
                    label = { Text(nature) }
                )
            }
        }
    }
}

@Composable
private fun CourseDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun EmptyCourseState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.DateRange,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "暂无课表数据",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "点击右下角按钮获取课表",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LoginDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("登录获取课表") },
        text = {
            Column {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("学号") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(username, password) },
                enabled = username.isNotBlank() && password.isNotBlank()
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
