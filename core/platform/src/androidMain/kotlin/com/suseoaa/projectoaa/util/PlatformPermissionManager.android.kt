package com.suseoaa.projectoaa.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Android平台的权限管理器实现
 */
actual class PlatformPermissionManager actual constructor() : KoinComponent {
    private val context: Context by inject()

    /**
     * 根据系统版本返回所需的权限列表
     */
    private fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12及以上版本需要附近设备权限
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.NEARBY_WIFI_DEVICES // 针对NSD/Wi-Fi发现
            )
        } else {
            // Android 11及以下版本需要精确位置权限
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    actual fun hasNearFieldPermissions(): Boolean {
        return getRequiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    @Composable
    actual fun RequestNearFieldPermissions(onResult: (Boolean) -> Unit) {
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.values.all { it }
            onResult(allGranted)
        }

        LaunchedEffect(Unit) {
            launcher.launch(getRequiredPermissions())
        }
    }

    actual fun isHardwareEnabled(): Boolean {
        return try {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
            val isWifiEnabled = wifiManager?.isWifiEnabled == true

            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
            val isBluetoothEnabled = bluetoothManager?.adapter?.isEnabled == true

            // 只要Wi-Fi开启即可满足NSD需求，如果后续支持BLE则也需要检查蓝牙
            isWifiEnabled || isBluetoothEnabled
        } catch (e: SecurityException) {
            // 如果发生权限异常，保守起见返回false，引导用户去检查设置
            false
        }
    }

    @Composable
    actual fun RequestEnableHardware(onResult: (Boolean) -> Unit) {
        val context = LocalContext.current
        val wifiLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) {
            onResult(isHardwareEnabled())
        }

        LaunchedEffect(Unit) {
            // Android 10+ 建议使用 Settings Panel
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val intent = android.content.Intent(android.provider.Settings.Panel.ACTION_WIFI)
                wifiLauncher.launch(intent)
            } else {
                val intent = android.content.Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
                wifiLauncher.launch(intent)
            }
        }
    }
}
