package com.suseoaa.projectoaa.login.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.suseoaa.projectoaa.login.model.UserInfoData
import com.suseoaa.projectoaa.login.viewmodel.ProfileViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.fetchUserInfo()
    }

    val isEditing = viewModel.isEditing

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "修改资料" else "个人中心", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (isEditing) {
                        // 编辑模式下：左上角是取消
                        IconButton(onClick = { viewModel.cancelEditing() }) {
                            Icon(Icons.Default.Close, contentDescription = "取消")
                        }
                    }
                },
                actions = {
                    if (!isEditing) {
                        // 查看模式下：右上角是编辑
                        IconButton(onClick = { viewModel.startEditing() }) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑")
                        }
                    } else {
                        // 编辑模式下：右上角也是保存
                        IconButton(onClick = { viewModel.saveUserInfo() }) {
                            Icon(Icons.Default.Check, contentDescription = "保存")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            )
        }
    ) { paddingValues ->
        ProfileContent(
            viewModel = viewModel,
            paddingValues = paddingValues,
            onLogout = onLogout
        )
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
private fun ProfileContent(
    viewModel: ProfileViewModel,
    paddingValues: PaddingValues,
    onLogout: () -> Unit
) {
    val screenConfig = LocalConfiguration.current
    val isWideScreen = screenConfig.screenWidthDp >= 600

    val userInfo = viewModel.userInfo
    val isLoading = viewModel.isLoading
    val errorMsg = viewModel.errorMessage
    val isEditing = viewModel.isEditing

    // === 监听修改密码弹窗 ===
    if (viewModel.showPasswordDialog) {
        ChangePasswordDialog(
            viewModel = viewModel,
            onConfirm = {
                // 调用修改密码逻辑
                viewModel.updatePassword(onSuccess = onLogout)
            },
            onDismiss = {
                viewModel.showPasswordDialog = false
                viewModel.newPasswordInput = ""
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.TopCenter
    ) {
        if (isLoading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else if (errorMsg != null) {
            // 错误提示
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = errorMsg, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.fetchUserInfo() }) { Text("重试") }
            }
        } else {
            userInfo?.let { user ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                        .widthIn(max = 1000.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. 头部
                    UserInfoHeader(user, isEditing)

                    Spacer(modifier = Modifier.height(32.dp))

                    // 2. 根据状态显示不同表单
                    if (isEditing) {
                        EditProfileForm(viewModel, isWideScreen)
                    } else {
                        ViewProfileContent(user, isWideScreen)
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // 3. 底部按钮区域
                    Column(
                        modifier = Modifier
                            .widthIn(max = 400.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp) // 按钮间距
                    ) {
                        if (isEditing) {
                            Button(
                                onClick = { viewModel.saveUserInfo() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("保存修改", fontSize = 18.sp)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.showPasswordDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("修改密码", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }

                            // 退出登录：调用 ViewModel 清除数据并跳转
                            RealLogoutButton(onClick = {
                                onLogout()
                            })
                        }
                    }
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}

// === 修改密码弹窗组件 ===
@Composable
fun ChangePasswordDialog(
    viewModel: ProfileViewModel,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改密码") },
        text = {
            Column {
                Text("请输入您的新密码：", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = viewModel.newPasswordInput,
                    onValueChange = { viewModel.newPasswordInput = it },
                    label = { Text("新密码") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    // 如果需要隐藏密码，可以解开下面这行的注释
                    // visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                )
                // 如果 ViewModel 有错误信息且弹窗正在显示，则展示
                if (viewModel.errorMessage != null && viewModel.showPasswordDialog) {
                    Text(
                        text = viewModel.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !viewModel.isLoading
            ) {
                Text(if (viewModel.isLoading) "提交中..." else "确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

// === 查看模式的内容 ===
@Composable
private fun ViewProfileContent(user: UserInfoData, isWideScreen: Boolean) {
    val basicInfoItems = listOf(
        Triple("姓名", user.name, Icons.Default.Person),
        Triple("学号", user.student_id.toString(), Icons.Default.Badge),
        Triple("用户名", user.username, Icons.Default.AccountCircle),
    )
    val academicInfoItems = listOf(
        Triple("所在部门", user.department, Icons.Default.School),
        Triple("当前状态", "已认证", Icons.Default.Verified),
        Triple("身份角色", user.role, Icons.Default.AdminPanelSettings),
    )

    if (isWideScreen) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                InfoSectionTitle("基础信息")
                DetailInfoCard(items = basicInfoItems)
            }
            Column(modifier = Modifier.weight(1f)) {
                InfoSectionTitle("学籍档案")
                DetailInfoCard(items = academicInfoItems)
            }
        }
    } else {
        InfoSectionTitle("基础信息")
        DetailInfoCard(items = basicInfoItems)
        Spacer(modifier = Modifier.height(24.dp))
        InfoSectionTitle("学籍档案")
        DetailInfoCard(items = academicInfoItems)
    }
}

// === 编辑模式的表单 (锁定关键字段) ===
@Composable
private fun EditProfileForm(viewModel: ProfileViewModel, isWideScreen: Boolean) {
    val roleOptions = listOf("理事会", "干事", "会员")

    // 定义基础输入块
    val BasicInputs = @Composable {
        // 允许修改：姓名
        EditTextField(
            label = "姓名",
            value = viewModel.editName,
            onValueChange = { viewModel.editName = it },
            icon = Icons.Default.Person
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 🔒 锁定：学号 (后端 Bug 保护)
        EditTextField(
            label = "学号 (不可修改)",
            value = viewModel.editStudentId,
            onValueChange = { }, // 禁止修改
            icon = Icons.Default.Badge,
            isReadOnly = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 🔒 锁定：用户名 (后端 Bug 保护)
        EditTextField(
            label = "用户名 (不可修改)",
            value = viewModel.editUsername,
            onValueChange = { }, // 禁止修改
            icon = Icons.Default.AccountCircle,
            isReadOnly = true
        )
    }

    // 定义学籍输入块
    val AcademicInputs = @Composable {
        // 允许修改：学院
        EditTextField(
            label = "所在学院",
            value = viewModel.editDepartment,
            onValueChange = { viewModel.editDepartment = it },
            icon = Icons.Default.School
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 允许修改：职位
        RoleDropdownMenu(
            selectedRole = viewModel.editRole,
            options = roleOptions,
            onRoleSelected = { viewModel.editRole = it }
        )
    }

    if (isWideScreen) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                InfoSectionTitle("修改基础信息")
                BasicInputs()
            }
            Column(modifier = Modifier.weight(1f)) {
                InfoSectionTitle("修改学籍信息")
                AcademicInputs()
            }
        }
    } else {
        InfoSectionTitle("修改基础信息")
        BasicInputs()
        Spacer(modifier = Modifier.height(24.dp))
        InfoSectionTitle("修改学籍信息")
        AcademicInputs()
    }
}

// === 通用输入框 (已修改：支持只读模式) ===
@Composable
private fun EditTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    isReadOnly: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        readOnly = isReadOnly, // 设为只读
        enabled = !isReadOnly, // 禁用交互，使其变灰
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            // 保证禁用状态下文字依然清晰可见
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
    )
}

// === 职位下拉选择组件 ===
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoleDropdownMenu(
    selectedRole: String,
    options: List<String>,
    onRoleSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            readOnly = true,
            value = selectedRole,
            onValueChange = {},
            label = { Text("职位") },
            leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onRoleSelected(option)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

// === 头部信息组件 ===
@Composable
private fun UserInfoHeader(userInfo: UserInfoData, isEditing: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(16.dp))
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            )
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        if (userInfo.avatar.isNullOrEmpty()) {
                            Brush.verticalGradient(
                                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                            )
                        } else {
                            androidx.compose.ui.graphics.SolidColor(Color.White)
                        }
                    )
                    .border(4.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!userInfo.avatar.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(userInfo.avatar)
                            .crossfade(true)
                            .build(),
                        contentDescription = "用户头像",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "用户头像",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(60.dp)
                    )
                }
            }

            if (isEditing) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(6.dp)
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "更改头像",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isEditing) "正在编辑..." else userInfo.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (!isEditing) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(50),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = userInfo.role,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp, start = 4.dp)
    )
}

@Composable
private fun DetailInfoCard(items: List<Triple<String, String, ImageVector>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items.forEachIndexed { index, (label, value, icon) ->
                InfoRow(label = label, value = value, icon = icon)
                if (index < items.lastIndex) {
                    Divider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(start = 68.dp, end = 24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun RealLogoutButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error,
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
    ) {
        Icon(Icons.Default.ExitToApp, null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "退出登录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}