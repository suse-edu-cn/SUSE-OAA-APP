package com.suseoaa.projectoaa

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.suseoaa.projectoaa.util.CaptchaOcrRecognizer
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 手机端锁定竖屏，平板端保持可旋转
        if (resources.configuration.smallestScreenWidthDp < 600) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        enableEdgeToEdge()

        // 初始化验证码识别器 (加载 ddddocr 模型)
        lifecycleScope.launch {
            CaptchaOcrRecognizer.initialize(this@MainActivity)
        }

        setContent {
            App()
        }
    }
}
