package com.suseoaa.projectoaa.app

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
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
            val startDest by mainViewModel.startDestination.collectAsStateWithLifecycle()
//            手机不允许自动旋转
            val isPhone = resources.configuration.smallestScreenWidthDp < 600
            requestedOrientation = if (isPhone) {
                // 手机：强制竖屏
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                // 平板：跟随用户/传感器
                ActivityInfo.SCREEN_ORIENTATION_USER
            }
            CompositionLocalProvider(LocalWindowSizeClass provides windowSizeClass) {
                ProjectOAATheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (startDest != "loading_route") {
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
}