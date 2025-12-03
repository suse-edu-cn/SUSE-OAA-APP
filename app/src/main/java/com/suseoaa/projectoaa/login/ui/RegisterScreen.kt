package com.suseoaa.projectoaa.login.ui


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.suseoaa.projectoaa.login.viewmodel.MainViewModel

@Composable
fun RegisterScreen(navController: NavController, viewModel: MainViewModel) {
    // 表单状态
    var studentid by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // 本地校验状态
    var passwordError by remember { mutableStateOf<String?>(null) }

    // 统一圆角样式 (24dp)
    val cardShape = RoundedCornerShape(24.dp)


    // 监听注册成功，自动返回 ===
    LaunchedEffect(viewModel.uiState) {
        if (viewModel.uiState.contains("成功")) {
            kotlinx.coroutines.delay(1000)
            viewModel.clearState()
            navController.popBackStack() // 自动返回登录页
        }
    }

    // 提交逻辑封装
    fun submitRegistration() {
        passwordError = null
        if (password != confirmPassword) {
            passwordError = "两次输入的密码不一致"
            return
        }
        if (password.length < 6) {
            passwordError = "密码长度至少6位"
            return
        }
        // 核心改动：role 默认传 "会员"
        viewModel.register(studentid, name, username, password, "会员")
    }

    // 响应式布局容器
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // === 关键修复：只有在宽度 > 600dp 且 宽度 > 高度 (横屏) 时才使用分栏布局 ===
        // 这样平板竖屏时会回退到下面的单列布局，避免布局崩坏
        val useSplitLayout = this.maxWidth > 600.dp && this.maxWidth > this.maxHeight

        if (useSplitLayout) {
            // === 平板/横屏布局 (左右分栏) ===
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：海报区
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // 使用 AccountCircle (核心库安全图标)
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "注册 Project OAA",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // 右侧：表单区
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = cardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(
                            alpha = 0.9f
                        )
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 48.dp) // 增加内边距
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        RegisterFormContent(
                            studentid,
                            name,
                            username,
                            password,
                            confirmPassword,
                            passwordError,
                            { studentid = it },
                            { name = it },
                            { username = it },
                            { password = it },
                            { confirmPassword = it },
                            viewModel,
                            navController,
                            ::submitRegistration
                        )
                    }
                }
            }
        } else {
            // === 手机/平板竖屏布局 (垂直堆叠) ===
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(40.dp))
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "注册 Project OAA",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(32.dp))

                Card(
                    shape = cardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(
                            alpha = 0.9f
                        )
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(32.dp)) {
                        RegisterFormContent(
                            studentid,
                            name,
                            username,
                            password,
                            confirmPassword,
                            passwordError,
                            { studentid = it },
                            { name = it },
                            { username = it },
                            { password = it },
                            { confirmPassword = it },
                            viewModel,
                            navController,
                            ::submitRegistration
                        )
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

// === 抽离的表单内容 (保持样式统一) ===
@Composable
fun RegisterFormContent(
    studentid: String,
    name: String,
    username: String,
    pass: String,
    confirmPass: String,
    passError: String?,
    onIdChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onUserChange: (String) -> Unit,
    onPassChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    viewModel: MainViewModel,
    navController: NavController,
    onSubmit: () -> Unit
) {
    // 统一输入框圆角 (12dp)
    val fieldShape = RoundedCornerShape(12.dp)

    // 学号 (使用 Info 图标)
    OutlinedTextField(
        value = studentid, onValueChange = onIdChange,
        label = { Text("学号") },
        leadingIcon = { Icon(Icons.Default.Info, null) },
        modifier = Modifier.fillMaxWidth(), singleLine = true, shape = fieldShape,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
    Spacer(modifier = Modifier.height(16.dp))

    // 姓名 (使用 Person 图标)
    OutlinedTextField(
        value = name, onValueChange = onNameChange,
        label = { Text("姓名") },
        leadingIcon = { Icon(Icons.Default.Person, null) },
        modifier = Modifier.fillMaxWidth(), singleLine = true, shape = fieldShape
    )
    Spacer(modifier = Modifier.height(16.dp))

    // 用户名 (使用 AccountBox 图标)
    OutlinedTextField(
        value = username, onValueChange = onUserChange,
        label = { Text("设置用户名") },
        leadingIcon = { Icon(Icons.Default.AccountBox, null) },
        modifier = Modifier.fillMaxWidth(), singleLine = true, shape = fieldShape
    )
    Spacer(modifier = Modifier.height(16.dp))

    // 密码
    OutlinedTextField(
        value = pass, onValueChange = onPassChange,
        label = { Text("设置密码") },
        leadingIcon = { Icon(Icons.Default.Lock, null) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(), singleLine = true, shape = fieldShape
    )
    Spacer(modifier = Modifier.height(16.dp))

    // 确认密码
    OutlinedTextField(
        value = confirmPass, onValueChange = onConfirmChange,
        label = { Text("确认密码") },
        leadingIcon = { Icon(Icons.Default.Lock, null) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(), singleLine = true, shape = fieldShape,
        isError = passError != null,
        supportingText = {
            if (passError != null) Text(
                passError,
                color = MaterialTheme.colorScheme.error
            )
        }
    )

    Spacer(modifier = Modifier.height(32.dp))

    // 注册按钮 (圆角 16dp)
    Button(
        onClick = onSubmit,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = !viewModel.isLoading,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(if (viewModel.isLoading) "注册中..." else "立即注册", fontSize = 18.sp)
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 返回登录
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        TextButton(onClick = {
            viewModel.clearState()
            navController.popBackStack()
        }) {
            Text("已有账号？返回登录")
        }
    }

    // 全局错误提示
    if (viewModel.uiState.isNotEmpty() && passError == null) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = viewModel.uiState,
            color = if (viewModel
                    .uiState.contains("成功")
            ) MaterialTheme
                .colorScheme
                .primary
            else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}