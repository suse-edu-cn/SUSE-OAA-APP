package com.suseoaa.projectoaa.ui.screen.usermanagement


import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.presentation.usermanagement.UserManagementViewModel
import com.suseoaa.projectoaa.shared.domain.model.person.UserQueryData
import com.suseoaa.projectoaa.ui.component.common.AdaptivePageScaffold
import com.suseoaa.projectoaa.ui.theme.AppDimensions
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: UserManagementViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var editingUser by remember { mutableStateOf<UserQueryData?>(null) }
    var deptFilter by remember { mutableStateOf("") }
    var nameFilter by remember { mutableStateOf("") }
    var roleFilter by remember { mutableStateOf("") }

    LaunchedEffect(uiState.updateMessage, uiState.error) {
        // 这里可以结合 snackbar 提示
    }

    AdaptivePageScaffold(
        sharedTransitionKey = "user_management_feature",
        title = "权利的游戏",
        onBack = onNavigateBack,
        compactContent = { modifier ->
            UserManagementContent(
                modifier = modifier,
                uiState = uiState,
                nameFilter = nameFilter,
                onNameFilterChange = { nameFilter = it },
                deptFilter = deptFilter,
                onDeptFilterChange = { deptFilter = it },
                roleFilter = roleFilter,
                onSearch = {
                    viewModel.updateFilters(
                        department = deptFilter,
                        name = nameFilter,
                        role = roleFilter
                    )
                    viewModel.fetchUsers()
                },
                onEditClick = { editingUser = it },
                viewModel = viewModel
            )
        },
        tabletContent = { modifier ->
            // 平板专属布局：左侧查询栏，右侧结果列表
            Row(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 左侧筛选栏
                Card(
                    modifier = Modifier
                        .weight(0.3f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                    ) {
                        Text(
                            "筛选条件",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        OutlinedTextField(
                            value = nameFilter,
                            onValueChange = { nameFilter = it },
                            label = { Text("姓名筛选") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        )
                        OutlinedTextField(
                            value = deptFilter,
                            onValueChange = { deptFilter = it },
                            label = { Text("部门筛选") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        )
                        Button(
                            onClick = {
                                viewModel.updateFilters(
                                    department = deptFilter,
                                    name = nameFilter,
                                    role = roleFilter
                                )
                                viewModel.fetchUsers()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("查询")
                        }
                    }
                }

                // 右侧结果列表
                Box(
                    modifier = Modifier
                        .weight(0.7f)
                        .fillMaxSize()
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement
                                .spacedBy(8.dp)
                        ) {
                            items(uiState.users) { user ->
                                UserCard(
                                    user = user,
                                    canEdit = viewModel.canEditUser(user.role),
                                    onEditClick = { editingUser = user }
                                )
                            }
                        }
                    }
                }
            }
        }
    )

    // 编辑弹窗

    editingUser?.let { userToEdit ->
        var editName by remember { mutableStateOf(userToEdit.name) }
        var editRole by remember { mutableStateOf(userToEdit.role) }
        var editDept by remember { mutableStateOf(userToEdit.department) }

        AlertDialog(
            containerColor = MaterialTheme.colorScheme.background,
            onDismissRequest = { editingUser = null },
            title = { Text("修改用户信息") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("姓名") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = editDept,
                        onValueChange = { editDept = it },
                        label = { Text("部门") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = editRole,
                        onValueChange = { editRole = it },
                        label = { Text("职位") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateUsers(
                        listOf(
                            userToEdit.copy(
                                name = editName,
                                role = editRole,
                                department = editDept
                            )
                        )
                    )
                    editingUser = null
                }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingUser = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun UserManagementContent(
    modifier: Modifier = Modifier,
    uiState: com.suseoaa.projectoaa.presentation.usermanagement.UserManagementUiState,
    nameFilter: String,
    onNameFilterChange: (String) -> Unit,
    deptFilter: String,
    onDeptFilterChange: (String) -> Unit,
    roleFilter: String,
    onSearch: () -> Unit,
    onEditClick: (UserQueryData) -> Unit,
    viewModel: UserManagementViewModel
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 筛选栏
        var isFilterExpanded by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .animateContentSize(),
            elevation = CardDefaults.cardElevation(0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { isFilterExpanded = !isFilterExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "筛选条件",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Icon(
                        imageVector = if (isFilterExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isFilterExpanded) "收起筛选" else "展开筛选"
                    )
                }

                if (isFilterExpanded) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        OutlinedTextField(
                            value = nameFilter,
                            onValueChange = onNameFilterChange,
                            label = { Text("姓名筛选") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = deptFilter,
                            onValueChange = onDeptFilterChange,
                            label = { Text("部门筛选") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                        Button(
                            onClick = onSearch,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("查询")
                        }
                    }
                }
            }
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.users) { user ->
                    UserCard(
                        user = user,
                        canEdit = viewModel.canEditUser(user.role),
                        onEditClick = { onEditClick(user) }
                    )
                }
            }
        }
    }
}

@Composable
fun UserCard(
    user: UserQueryData,
    canEdit: Boolean,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp), // 外边距，留出阴影空间
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            // 确保背景颜色随主题变化，通常 Card 使用 surface 或 surfaceContainer
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp, // 适当的高度，8dp 在列表中可能过于突兀
            pressedElevation = 8.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp), // 内部填充
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name.ifEmpty { "未命名" },
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface // 确保文字颜色适配主题
                )
                Text(
                    text = "学号: ${user.studentId}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "部门: ${user.department}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "职位: ${user.role}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (canEdit) {
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "编辑",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}