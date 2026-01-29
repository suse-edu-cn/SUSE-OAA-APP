package com.suseoaa.projectoaa

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.compose.rememberNavController
import com.suseoaa.projectoaa.presentation.MainViewModel
import com.suseoaa.projectoaa.ui.navigation.AppNavHost
import com.suseoaa.projectoaa.ui.navigation.Screen
import com.suseoaa.projectoaa.ui.theme.ProjectOAATheme
import com.suseoaa.projectoaa.util.ToastHandler
import org.koin.compose.viewmodel.koinViewModel

// 亮色渐变
private val LightGradientColors = listOf(
    Color(0xFF9BDCE5),
    Color(0xFF8EC5FC),
)

// 暗色渐变
private val DarkGradientColors = listOf(
    Color(0xFF1A3A4A),
    Color(0xFF1A2A4A),
)

@Composable
fun App(
    mainViewModel: MainViewModel = koinViewModel()
) {
    val startDestination by mainViewModel.startDestination.collectAsState()
    
    ProjectOAATheme {
        // 全局 Toast 处理器
        ToastHandler()
        
        // 等待加载完成
        if (startDestination == null) {
            val isDarkTheme = isSystemInDarkTheme()
            val gradientColors = if (isDarkTheme) DarkGradientColors else LightGradientColors
            val headerTextColor = if (isDarkTheme) Color.White else Color.Black
            
            // 启动加载界面 - 渐变背景
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = gradientColors + MaterialTheme.colorScheme.background
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "青蟹",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = headerTextColor.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "致力服务于四川轻化工大学开放原子开源协会",
                        style = MaterialTheme.typography.bodyMedium,
                        color = headerTextColor.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            val navController = rememberNavController()
            AppNavHost(
                navController = navController,
                startDestination = startDestination!!
            )
        }
    }
}
