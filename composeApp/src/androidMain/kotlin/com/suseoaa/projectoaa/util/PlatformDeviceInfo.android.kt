package com.suseoaa.projectoaa.util

import android.app.ActivityManager
import android.content.Context
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES10
import android.os.Build
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform

actual object PlatformDeviceInfo {
    actual fun queryDeviceInfo(): DeviceInfo {
        val context = KoinPlatform.getKoin().get<Context>()

        // ── RAM ──────────────────────────────────────────────────────────
        val memInfo = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(memInfo)
        val totalRam = memInfo.totalMem
        val availableRam = memInfo.availMem

        // ── CPU ──────────────────────────────────────────────────────────
        val cpuModel = buildString {
            // 尝试从 /proc/cpuinfo 解析 Hardware 字段
            try {
                val lines = java.io.File("/proc/cpuinfo").readLines()
                val hardware = lines.firstOrNull { it.startsWith("Hardware", ignoreCase = true) }
                    ?.substringAfter(":")?.trim()
                if (!hardware.isNullOrBlank()) {
                    append(hardware)
                    return@buildString
                }
            } catch (_: Exception) {}
            // 回退：Build.HARDWARE 或 MODEL
            append(Build.HARDWARE.ifBlank { Build.MODEL })
        }

        // ── SoC Vendor & NPU ─────────────────────────────────────────────
        val socVendorStr = Build.SOC_MANUFACTURER.takeIf { it.isNotBlank() } ?: inferSocVendor(cpuModel)
        val hardware = Build.HARDWARE.lowercase()
        val cpuLower = cpuModel.lowercase()
        val hasNpu: Boolean
        val npuDesc: String
        when {
            socVendorStr.contains("Qualcomm", ignoreCase = true) ||
            hardware.contains("qcom") || cpuLower.contains("snapdragon") -> {
                hasNpu = true
                npuDesc = "Hexagon NPU (Qualcomm)"
            }
            socVendorStr.contains("MediaTek", ignoreCase = true) ||
            cpuLower.contains("mediatek") || cpuLower.contains("dimensity") -> {
                hasNpu = true
                npuDesc = "APU (MediaTek)"
            }
            socVendorStr.contains("Samsung", ignoreCase = true) ||
            cpuLower.contains("exynos") -> {
                hasNpu = true
                npuDesc = "NPU (Samsung Exynos)"
            }
            else -> {
                hasNpu = false
                npuDesc = "未检测到专用 NPU"
            }
        }

        // ── GPU ──────────────────────────────────────────────────────────
        // 通过 EGL 创建一个极简 Surface 读取 GL_RENDERER 字符串
        val gpuRenderer = queryGpuRenderer()

        // ── Storage ──────────────────────────────────────────────────────
        val statFs = StatFs(context.filesDir.absolutePath)
        val totalStorage = statFs.totalBytes
        val availableStorage = statFs.availableBytes

        // ── OS Version ───────────────────────────────────────────────────
        val osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

        return DeviceInfo(
            totalRam = totalRam,
            availableRam = availableRam,
            cpuModel = cpuModel,
            gpuRenderer = gpuRenderer,
            hasNpu = hasNpu,
            npuDescription = npuDesc,
            totalStorage = totalStorage,
            availableStorage = availableStorage,
            osVersion = osVersion,
            socVendor = socVendorStr
        )
    }

    /** 从 CPU 型号字符串推断 SoC 厂商 */
    private fun inferSocVendor(cpuModel: String): String {
        val lower = cpuModel.lowercase()
        return when {
            lower.contains("snapdragon") || lower.contains("qcom") -> "Qualcomm"
            lower.contains("mediatek") || lower.contains("dimensity") || lower.contains("helio") -> "MediaTek"
            lower.contains("exynos") -> "Samsung"
            lower.contains("kirin") -> "HiSilicon"
            lower.contains("tensor") -> "Google"
            lower.contains("unisoc") -> "UNISOC"
            else -> Build.MANUFACTURER
        }
    }

    /**
     * 通过创建一个离屏 EGL 上下文来读取 OpenGL ES 的 GL_RENDERER 字符串。
     * 该操作会在调用线程上运行（应已处于后台线程）。
     */
    private fun queryGpuRenderer(): String {
        return try {
            val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (display == EGL14.EGL_NO_DISPLAY) return fallbackGpuName()

            val version = IntArray(2)
            if (!EGL14.eglInitialize(display, version, 0, version, 1)) return fallbackGpuName()

            val attribList = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            if (!EGL14.eglChooseConfig(display, attribList, 0, configs, 0, 1, numConfigs, 0) ||
                numConfigs[0] == 0) return fallbackGpuName()

            val contextAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
            val ctx = EGL14.eglCreateContext(display, configs[0], EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
            if (ctx == EGL14.EGL_NO_CONTEXT) return fallbackGpuName()

            val surfaceAttribs = intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE)
            val surface = EGL14.eglCreatePbufferSurface(display, configs[0], surfaceAttribs, 0)
            if (surface == EGL14.EGL_NO_SURFACE) {
                EGL14.eglDestroyContext(display, ctx)
                return fallbackGpuName()
            }

            EGL14.eglMakeCurrent(display, surface, surface, ctx)
            val renderer = GLES10.glGetString(GLES10.GL_RENDERER) ?: fallbackGpuName()

            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(display, surface)
            EGL14.eglDestroyContext(display, ctx)
            EGL14.eglTerminate(display)

            renderer
        } catch (_: Exception) {
            fallbackGpuName()
        }
    }

    private fun fallbackGpuName(): String = Build.HARDWARE.ifBlank { "Unknown GPU" }
}
