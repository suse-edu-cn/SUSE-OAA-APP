package com.suseoaa.projectoaa

import android.Manifest
import android.os.Bundle
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.suseoaa.projectoaa.reminder.CourseReminderScheduler
import com.suseoaa.projectoaa.util.CaptchaOcrRecognizer
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    companion object {
        // 测试开关：改为 true 后，启动应用 10 秒会触发最近一门课的提醒通知。
        private const val ENABLE_COURSE_REMINDER_TEST_ENTRY = false
    }

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            maybeRunCourseReminderTestEntry()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val hasNotificationPermission = checkAndRequestNotificationPermission()
        if (hasNotificationPermission) {
            maybeRunCourseReminderTestEntry()
        }

        // 初始化验证码识别器 (加载 ddddocr 模型)
        lifecycleScope.launch {
            CaptchaOcrRecognizer.initialize(this@MainActivity)
        }

        setContent {
            App()
        }
    }

    private fun checkAndRequestNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        return granted
    }

    private fun maybeRunCourseReminderTestEntry() {
        CourseReminderScheduler.scheduleTestReminderAfter10Seconds(
            context = this,
            enabled = ENABLE_COURSE_REMINDER_TEST_ENTRY
        )
    }
}
