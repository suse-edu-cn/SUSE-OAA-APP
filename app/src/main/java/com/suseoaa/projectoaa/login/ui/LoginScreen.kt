package com.suseoaa.projectoaa.login.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.suseoaa.projectoaa.common.navigation.AppRoutes
import com.suseoaa.projectoaa.login.viewmodel.MainViewModel

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: MainViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(key1 = viewModel.loginSuccess) {
        if (viewModel.loginSuccess) {
            onLoginSuccess()
            viewModel.clearState()
        }
    }

    val cardShape = RoundedCornerShape(24.dp)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useSplitLayout = this.maxWidth > 600.dp && this.maxWidth > this.maxHeight

        if (useSplitLayout) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧品牌区
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "登录 Project OAA",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // 右侧表单区
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
                            .padding(horizontal = 48.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LoginFormContent(
                            username,
                            password,
                            { username = it },
                            { password = it },
                            viewModel,
                            context,
                            navController
                        )
                    }
                }
            }
        } else {
            // === 手机/平板竖屏布局 (垂直堆叠) ===
            // 增加 verticalScroll 确保在小屏幕或键盘弹出时可滚动
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(40.dp))
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "登录 Project OAA",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(48.dp))

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
                        LoginFormContent(
                            username,
                            password,
                            { username = it },
                            { password = it },
                            viewModel,
                            context,
                            navController
                        )
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

// === LoginFormContent 保持不变 ===
@Composable
fun LoginFormContent(
    user: String, pass: String,
    onUserChange: (String) -> Unit,
    onPassChange: (String) -> Unit,
    viewModel: MainViewModel,
    context: android.content.Context,
    navController: NavController
) {
    val fieldShape = RoundedCornerShape(12.dp)
    OutlinedTextField(
        value = user, onValueChange = onUserChange,
        label = { Text("用户名") },
        leadingIcon = { Icon(Icons.Default.AccountBox, null) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = fieldShape
    )
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(
        value = pass, onValueChange = onPassChange,
        label = { Text("密码") },
        leadingIcon = { Icon(Icons.Default.Lock, null) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = fieldShape
    )
    Spacer(modifier = Modifier.height(32.dp))
    Button(
        onClick = { viewModel.login(context, user, pass) },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = !viewModel.isLoading,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(if (viewModel.isLoading) "登录中..." else "登录", fontSize = 18.sp)
    }
    Spacer(modifier = Modifier.height(16.dp))
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        TextButton(onClick = {
            viewModel.clearState()
            navController.navigate(AppRoutes.Register.route)
        }) {
            Text("没有账号？去注册")
        }
    }
    if (viewModel.uiState.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = viewModel.uiState,
            color = if (viewModel.uiState.contains("成功")) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}