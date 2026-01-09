package com.suseoaa.projectoaa.feature.register

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = " 注册",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "注意：学号填写后无法修改",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Red,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )
        OutlinedTextField(
            value = viewModel.studentID,
            onValueChange = {
                if (it.length <= 11) viewModel.studentID = it
            },
            label = { Text("学号") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp),
//            单行模式
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
//                软键盘右下角显示下一步
                imeAction = ImeAction.Next
            )
        )
        OutlinedTextField(
            value = viewModel.realName,
            onValueChange = {
                if (it.length <= 11) viewModel.realName = it
            },
            label = { Text("姓名") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp),
//            单行模式
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
//                软键盘右下角显示下一步
                imeAction = ImeAction.Next
            )
        )

        OutlinedTextField(
            value = viewModel.userName,
            onValueChange = {
                if (it.length <= 11) viewModel.userName = it
            },
            label = { Text("用户名") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp),
//            单行模式
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
//                软键盘右下角显示下一步
                imeAction = ImeAction.Next
            )
        )

        OutlinedTextField(
            value = viewModel.password,
            onValueChange = {
                if (it.length <= 11) viewModel.password = it
            },
            label = { Text("密码") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp),
//            单行模式
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
//                软键盘右下角显示下一步
                imeAction = ImeAction.Next
            )
        )

        OutlinedTextField(
            value = viewModel.confirmPassword,
            onValueChange = {
                if (it.length <= 11) viewModel.confirmPassword = it
            },
            label = { Text("确认密码") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp),
//            单行模式
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
//                软键盘右下角显示下一步
                imeAction = ImeAction.Next
            )
        )
        Spacer(modifier = Modifier.height(24.dp))
        //        登录按钮
        Button(
            onClick = {
                viewModel.register(
                    onSuccess = {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        onRegisterSuccess()
                    },
                    onError = {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            enabled = !viewModel.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(horizontal = 30.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("注册")
            }
        }
    }
}