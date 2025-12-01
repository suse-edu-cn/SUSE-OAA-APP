package com.suseoaa.projectoaa

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge // [关键] 引入这个
// 启动动画相关的 Imports
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
// 响应式布局 (WindowSizeClass) Imports
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import com.suseoaa.projectoaa.common.navigation.AppNavigation
import com.suseoaa.projectoaa.common.theme.ProjectOAATheme
import com.suseoaa.projectoaa.common.util.WallpaperManager
import com.suseoaa.projectoaa.startHomeNavigation.viewmodel.ShareViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * App 的唯一入口 Activity。
 * 负责：
 * 1. Hilt 依赖注入的入口 (@AndroidEntryPoint)。
 * 2. 初始化全局服务 (如 WallpaperManager)。
 * 3. 设置 Compose UI 内容 (ProjectOAATheme, AppNavigation)。
 * 4. 创建全局共享的 [ShareViewModel]。
 * 5. 计算窗口尺寸类 (WindowSizeClass) 以实现响应式布局。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // [关键修改] 开启 Edge-to-Edge，让应用内容能绘制到状态栏下方
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        // 1. 异步初始化壁纸管理器：在IO线程执行，避免阻塞UI。
        lifecycleScope.launch(Dispatchers.IO) { WallpaperManager.initialize(applicationContext) }

        setContent {
            // 2. [关键] 创建全局唯一的 ShareViewModel 实例。
            // 此实例将通过 AppNavigation 传递给所有需要共享状态的屏幕。
            val shareViewModel: ShareViewModel = hiltViewModel()

            // 从 ViewModel 中读取当前保存的主题配置
            val targetTheme = shareViewModel.currentTheme

            // 应用全局主题 (包含动态色彩和二次元主题逻辑)
            ProjectOAATheme(themeConfig = targetTheme) {

                // 3. 计算窗口尺寸类，用于响应式布局 (Compact, Medium, Expanded)。
                val windowSizeClass = calculateWindowSizeClass(this)

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    // 保持 Surface 透明，以便 ShareViewModel 控制的壁纸可见
                    color = Color.Transparent
                ) {
                    val context = LocalContext.current

                    // 4. [全局事件] 监听来自 ShareViewModel 的一次性 Toast 事件。
                    // 放在 Activity 级别可确保在任何屏幕都能显示 Toast。
                    LaunchedEffect(shareViewModel) {
                        shareViewModel.toastEvent.collectLatest { message ->
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    }

                    // 5. [启动动画] 状态：控制 App 内容是否可见。
                    var isVisible by remember { mutableStateOf(false) }

                    // 6. [启动动画] 触发器：Composable 首次加载时，延迟片刻后将 isVisible 设为 true。
                    // 延迟(delay)有助于确保UI渲染准备就绪，使动画更平滑。
                    LaunchedEffect(Unit) {
                        delay(100) // 100毫秒延迟
                        isVisible = true
                    }

                    // 7. [启动动画] 容器：当 isVisible 变为 true 时，内容会以淡入和滑入的方式出现。
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(animationSpec = tween(durationMillis = 400)) +
                                slideInVertically(
                                    animationSpec = tween(durationMillis = 400),
                                    initialOffsetY = { it / 20 } // 从顶部轻微滑入 (5%的高度)
                                ),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 8. 渲染 App 的主导航和内容。
                        // 传入 windowSizeClass 以便 AdaptiveApp 切换布局。
                        // 传入 shareViewModel 以共享状态。
                        AppNavigation(
                            windowSizeClass = windowSizeClass.widthSizeClass,
                            shareViewModel = shareViewModel
                        )
                    }
                }
            }
        }
    }
}