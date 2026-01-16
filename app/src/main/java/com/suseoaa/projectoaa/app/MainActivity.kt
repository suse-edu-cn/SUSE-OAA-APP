package com.suseoaa.projectoaa.app

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suseoaa.projectoaa.core.data.repository.AppUpdateRepository
import com.suseoaa.projectoaa.core.designsystem.theme.ProjectOAATheme
import com.suseoaa.projectoaa.feature.home.OaaApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

val LocalWindowSizeClass = staticCompositionLocalOf<WindowSizeClass> {
    error("No WindowSizeClass provided")
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    // 注入 Repository，用于确认下载ID和调用安装逻辑
    @Inject
    lateinit var appUpdateRepository: AppUpdateRepository

    // 定义广播接收器，监听下载完成事件
    private val downloadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                // 检查是否是当前 App 发起的更新任务
                if (id != -1L && id == appUpdateRepository.currentDownloadId) {
                    // 调用 Repository 中的安装方法
                    appUpdateRepository.installApkById(id)
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 注册广播
        ContextCompat.registerReceiver(
            this,
            downloadCompleteReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            // 适配 Android 13+
            ContextCompat.RECEIVER_EXPORTED
        )

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val startDest by mainViewModel.startDestination.collectAsStateWithLifecycle()
            // 手机不允许自动旋转
            val isPhone = resources.configuration.smallestScreenWidthDp < 600
            requestedOrientation = if (isPhone) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
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

    // 销毁时取消注册，避免内存泄漏
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(downloadCompleteReceiver)
    }
}