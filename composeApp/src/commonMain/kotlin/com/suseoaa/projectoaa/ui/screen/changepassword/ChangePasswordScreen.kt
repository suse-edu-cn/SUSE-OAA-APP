package com.suseoaa.projectoaa.ui.screen.changepassword

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.presentation.changepassword.ChangePasswordViewModel
import com.suseoaa.projectoaa.ui.component.BackButton
import com.suseoaa.projectoaa.util.showToast
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    onSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ChangePasswordViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isOldPasswordVisible by remember { mutableStateOf(false) }
    var isNewPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    // 监听修改成功
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onSuccess()
        }
    }

    // 显示错误/成功提示
    uiState.errorMessage?.let { message ->
        showToast(message)
        LaunchedEffect(message) {
            viewModel.clearMessages()
        }
    }
    uiState.successMessage?.let { message ->
        showToast(message)
        LaunchedEffect(message) {
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("修改密码") },
                navigationIcon = {
                    BackButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // 旧密码输入框
            OutlinedTextField(
                value = uiState.oldPassword,
                onValueChange = viewModel::updateOldPassword,
                label = { Text("当前密码") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                visualTransformation = if (isOldPasswordVisible)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                trailingIcon = {
                    IconButton(onClick = { isOldPasswordVisible = !isOldPasswordVisible }) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = if (isOldPasswordVisible) "隐藏密码" else "显示密码",
                            tint = if (isOldPasswordVisible) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 新密码输入框
            OutlinedTextField(
                value = uiState.newPassword,
                onValueChange = viewModel::updateNewPassword,
                label = { Text("新密码") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                visualTransformation = if (isNewPasswordVisible)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                trailingIcon = {
                    IconButton(onClick = { isNewPasswordVisible = !isNewPasswordVisible }) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = if (isNewPasswordVisible) "隐藏密码" else "显示密码",
                            tint = if (isNewPasswordVisible) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 确认新密码输入框
            OutlinedTextField(
                value = uiState.confirmPassword,
                onValueChange = viewModel::updateConfirmPassword,
                label = { Text("确认新密码") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                visualTransformation = if (isConfirmPasswordVisible)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                trailingIcon = {
                    IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = if (isConfirmPasswordVisible) "隐藏密码" else "显示密码",
                            tint = if (isConfirmPasswordVisible) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 确认按钮
            Button(
                onClick = viewModel::changePassword,
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("确认修改")
                }
            }
        }
    }
}
