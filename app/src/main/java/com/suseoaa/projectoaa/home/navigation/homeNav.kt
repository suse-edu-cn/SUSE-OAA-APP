package com.suseoaa.projectoaa.home.navigation

import MainViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun AppNavigationHost(
    navController: NavHostController,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val greeting by viewModel.greeting.collectAsState()

    NavHost(
        navController = navController,
        startDestination = AppRoute.HOME.route,
        modifier = modifier
    ) {
        composable(AppRoute.HOME.route) {
            // 首页内容
            SimpleScreen(title = "首页", content = greeting)
        }
        composable(AppRoute.PROFILE.route) {
            // 个人中心内容
            SimpleScreen(title = "个人中心", content = "这里是用户资料设置")
        }
        composable(AppRoute.SETTINGS.route) {
            // 设置内容
            SimpleScreen(title = "设置", content = "App 配置选项")
        }
    }
}

// 一个简单的通用页面组件
@Composable
fun SimpleScreen(title: String, content: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = content, style = MaterialTheme.typography.bodyLarge)
    }
}