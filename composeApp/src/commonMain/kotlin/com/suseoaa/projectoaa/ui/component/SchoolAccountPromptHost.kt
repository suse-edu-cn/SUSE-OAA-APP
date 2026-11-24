package com.suseoaa.projectoaa.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.suseoaa.projectoaa.presentation.SchoolAccountPromptViewModel
import com.suseoaa.projectoaa.util.SchoolAccountPromptManager
import org.koin.compose.viewmodel.koinViewModel

/**
 * 全局「添加教务系统账号」弹窗宿主。
 */
@Composable
fun SchoolAccountPromptHost(
    viewModel: SchoolAccountPromptViewModel = koinViewModel()
) {
    val activeRequest by SchoolAccountPromptManager.activeRequest.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val request = activeRequest ?: return

    var studentId by remember(request) { mutableStateOf("") }
    var password by remember(request) { mutableStateOf("") }
    var passwordVisible by remember(request) { mutableStateOf(false) }

    LaunchedEffect(request) {
        viewModel.resetFormState()
    }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = {
            if (!uiState.isLoading) {
                viewModel.resetFormState()
                SchoolAccountPromptManager.dismiss()
            }
        },
        title = {
            Text(text = "添加教务系统账号")
        },
        text = {
            Column {
                val featureHint = request.featureName?.takeIf { it.isNotBlank() }?.let {
                    "使用「$it」需要登录教务系统，请添加您的教务系统账号。"
                } ?: "该功能需要登录教务系统，请添加您的教务系统账号。"
                Text(
                    text = featureHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!uiState.hasDefaultAccount) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "当前暂无默认教务账号，添加后将自动设为默认账号。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = studentId,
                    onValueChange = { studentId = it },
                    label = { Text("学号") },
                    singleLine = true,
                    enabled = !uiState.isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("教务系统密码") },
                    singleLine = true,
                    enabled = !uiState.isLoading,
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible },
                            enabled = !uiState.isLoading
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = if (passwordVisible) "隐藏密码" else "显示密码"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                uiState.statusMessage?.let { status ->
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                uiState.errorMessage?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.addAccount(studentId, password) },
                enabled = !uiState.isLoading && studentId.isNotBlank() && password.isNotBlank()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("登录并添加")
                }
            }
        },
        dismissButton = {
            if (!uiState.isLoading) {
                TextButton(
                    onClick = {
                        viewModel.resetFormState()
                        SchoolAccountPromptManager.dismiss()
                    }
                ) {
                    Text("取消")
                }
            }
        }
    )
}
