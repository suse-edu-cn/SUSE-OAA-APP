package com.suseoaa.projectoaa.login.ui


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // 监听登录成功事件
    LaunchedEffect(key1 = viewModel.loginSuccess) {
        if (viewModel.loginSuccess) {
            viewModel.clearState()
            onLoginSuccess()
        }
    }

    val cardShape = RoundedCornerShape(24.dp)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useSplitLayout = this.maxWidth > 600.dp && this.maxWidth > this.maxHeight

        if (useSplitLayout) {
            // 平板/横屏布局
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. 左侧品牌区
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Project:OAA", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("登录", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.outline)
                }

                // 2. 右侧表单区
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    shape = cardShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                            navController
                        )
                    }
                }
            }
        } else {
            // 手机/竖屏布局
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Project:OAA", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("登录", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(48.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = cardShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(32.dp)) {
                        LoginFormContent(
                            username,
                            password,
                            { username = it },
                            { password = it },
                            viewModel,
                            navController
                        )
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

// === 登录表单 ===
@Composable
fun LoginFormContent(
    user: String, pass: String,
    onUserChange: (String) -> Unit,
    onPassChange: (String) -> Unit,
    viewModel: MainViewModel,
    navController: NavController
) {
    val fieldShape = RoundedCornerShape(12.dp)

    Text(
        "登录",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(24.dp))
    OutlinedTextField(
        value = user,
        onValueChange = onUserChange,
        label = { Text("用户名") },
        modifier = Modifier.fillMaxWidth(),
        shape = fieldShape,
        singleLine = true
    )
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(
        value = pass,
        onValueChange = onPassChange,
        label = { Text("密码") },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
        shape = fieldShape,
        singleLine = true
    )
    Spacer(modifier = Modifier.height(32.dp))
    Button(
        onClick = { viewModel.login(user, pass) },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = !viewModel.isLoading,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(if (viewModel.isLoading) "登录中..." else "登录", fontSize = 18.sp)
    }

    // 错误/状态信息
    if (viewModel.uiState.isNotBlank()) {
        Text(
            viewModel.uiState,
            color = if (viewModel.loginSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
    }

    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(onClick = { /* TODO */ }) {
            Text("忘记密码？")
        }
        TextButton(onClick = { navController.navigate(AppRoutes.Register.route) }) {
            Text("注册账户")
        }
    }
}