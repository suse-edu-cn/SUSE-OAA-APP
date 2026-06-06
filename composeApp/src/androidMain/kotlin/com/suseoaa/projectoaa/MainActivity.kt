package com.suseoaa.projectoaa

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.suseoaa.projectoaa.shared.data.local.TokenManager
import com.suseoaa.projectoaa.util.CaptchaOcrRecognizer
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import com.suseoaa.projectoaa.util.DeepLinkManager
import android.content.Intent

class MainActivity : ComponentActivity() {
    private val tokenManager: TokenManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)

        val rootBackCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, rootBackCallback)

        lifecycleScope.launch {
            tokenManager.predictiveBackEnabledFlow.collect { isEnabled ->
                rootBackCallback.isEnabled = !isEnabled
            }
        }

        // 手机端锁定竖屏，平板端保持可旋转
        if (resources.configuration.smallestScreenWidthDp < 600) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        enableEdgeToEdge()

        // 初始化验证码识别器 (加载 ddddocr 模型)
        lifecycleScope.launch {
            CaptchaOcrRecognizer.initialize(this@MainActivity)
        }

        // 初始化模型下载器
        com.suseoaa.projectoaa.util.ModelDownloader.init(this@MainActivity)
        
        // 初始化 AI 引擎上下文
        com.suseoaa.projectoaa.shared.domain.engine.CampusAiEngine.initContext(this@MainActivity)

        // 请求通知权限并启动课程提醒服务
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val requestPermissionLauncher = registerForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    startCourseReminderService()
                }
            }
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } else {
                startCourseReminderService()
            }
        } else {
            startCourseReminderService()
        }

        setContent {
            App()
        }
    }

    private fun startCourseReminderService() {
        try {
            val serviceIntent = android.content.Intent(
                this,
                com.suseoaa.projectoaa.scheduling.CourseReminderService::class.java
            )
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data
        if (uri != null) {
            DeepLinkManager.pendingDeepLink.value = uri.toString()
        }
    }
}
