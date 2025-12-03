package com.suseoaa.projectoaa.common.util

import androidx.activity.ComponentActivity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat


class PermissionRequestActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PERMISSION = "extra_permission"
        const val EXTRA_IS_SETTINGS_FALLBACK = "extra_is_settings_fallback"

        fun createIntent(
            context: Context,
            permission: String?,
            isSettingsFallback: Boolean = false
        ): Intent {
            return Intent(context, PermissionRequestActivity::class.java).apply {
                putExtra(EXTRA_PERMISSION, permission)
                putExtra(EXTRA_IS_SETTINGS_FALLBACK, isSettingsFallback)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    // 注册权限请求的回调
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "权限已获取，请重试保存", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "未授予权限，无法保存", Toast.LENGTH_SHORT).show()
            }
            // 无论结果如何，都关闭这个透明 Activity
            finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permission = intent.getStringExtra(EXTRA_PERMISSION)
        val isSettingsFallback = intent.getBooleanExtra(EXTRA_IS_SETTINGS_FALLBACK, false)

        when {
            // 场景1：请求特定权限 (Android 9 及以下)
            permission != null -> {
                handlePermissionRequest(permission)
            }
            // 场景2：降级到“设置” (Android 10 及以上)
            isSettingsFallback -> {
                showSettingsDialog()
            }
            // 场景3：无效启动，直接关闭
            else -> {
                finish()
            }
        }
    }

    private fun handlePermissionRequest(permission: String) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            // 显示为什么需要此权限的解释
            AlertDialog.Builder(this)
                .setTitle("需要权限")
                .setMessage("为了将壁纸保存到您的相册，我们需要存储权限。")
                .setPositiveButton("好的") { _, _ ->
                    permissionLauncher.launch(permission)
                }
                .setNegativeButton("取消") { _, _ ->
                    Toast.makeText(this, "未授予权限，无法保存", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .setOnCancelListener { finish() } // 确保用户点击外部时也关闭
                .show()
        } else {
            // 直接请求权限
            permissionLauncher.launch(permission)
        }
    }

    private fun showSettingsDialog() {
        // Android 10+ 保存失败，引导用户到设置
        AlertDialog.Builder(this)
            .setTitle("保存失败")
            .setMessage("无法访问存储。请在应用设置中检查应用的存储权限。")
            .setPositiveButton("去设置") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", packageName, null)
                startActivity(intent)
                finish()
            }
            .setNegativeButton("取消") { _, _ ->
                finish()
            }
            .setOnCancelListener { finish() } // 确保用户点击外部时也关闭
            .show()
    }
}