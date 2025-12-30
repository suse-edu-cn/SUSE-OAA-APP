package com.suseoaa.projectoaa.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suseoaa.projectoaa.feature.home.OaaApp
import com.suseoaa.projectoaa.core.designsystem.theme.ProjectOAATheme
import dagger.hilt.android.AndroidEntryPoint

val LocalWindowSizeClass = staticCompositionLocalOf<WindowSizeClass> {
    error("No WindowSizeClass provided")
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)

            // 1. 获取 ViewModel 计算出的起始页状态
            val startDest by mainViewModel.startDestination.collectAsStateWithLifecycle()

            CompositionLocalProvider(LocalWindowSizeClass provides windowSizeClass) {
                ProjectOAATheme {
                    // 2. 只有当状态确定（不是 loading）时，才加载 UI
                    if (startDest != "loading_route") {
                        // 3. 只调用一次 OaaApp，并传入 startDestination
                        OaaApp(
                            windowSizeClass = windowSizeClass.widthSizeClass,
                            startDestination = startDest
                        )
                    }
                }
            }
        }
    }
}