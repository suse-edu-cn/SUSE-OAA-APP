package com.suseoaa.projectoaa.util

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSURL
import platform.UIKit.UIDevice
import platform.darwin.sysctlbyname
import platform.posix.size_tVar

/**
 * iOS 平台设备信息查询实现
 * 使用 UIDevice、sysctl 和 NSFileManager 读取硬件参数
 */
@OptIn(ExperimentalForeignApi::class)
actual object PlatformDeviceInfo {
    actual fun queryDeviceInfo(): DeviceInfo {
        val device = UIDevice.currentDevice

        // ── CPU 型号（通过 sysctl hw.machine 读取芯片标识） ──────────────
        val cpuModel = readSysctlString("hw.machine").let { machine ->
            // 将 Apple 芯片标识映射为可读型号
            when {
                machine.startsWith("iPhone") -> mapAppleChip(machine)
                machine.startsWith("iPad") -> mapAppleChip(machine)
                machine.contains("arm64") -> "Apple Silicon"
                else -> machine.ifBlank { "Apple SoC" }
            }
        }

        // ── RAM（通过 hw.memsize） ─────────────────────────────────────
        val totalRam = readSysctlLong("hw.memsize")
        // iOS 不直接暴露 available RAM，用总内存的近似估算
        val availableRam = totalRam / 2 // 保守估算，实际可用约为总量的 40~60%

        // ── GPU & NPU（Apple 设备统一使用 Apple Neural Engine） ─────────
        val gpuRenderer = "Apple GPU (${cpuModel})"
        val hasNpu = true // 所有支持 iOS 14+ 的设备均有 Neural Engine
        val npuDesc = "Apple Neural Engine (ANE)"

        // ── 存储空间（NSFileManager） ─────────────────────────────────
        val homeUrl = NSURL.fileURLWithPath(NSHomeDirectory())
        val attrs = NSFileManager.defaultManager.attributesOfFileSystemForPath(NSHomeDirectory(), null)
        val totalStorage = (attrs?.get("NSFileSystemSize") as? Long) ?: 0L
        val availableStorage = (attrs?.get("NSFileSystemFreeSize") as? Long) ?: 0L

        // ── OS 版本 ────────────────────────────────────────────────────
        val osVersion = "iOS ${device.systemVersion}"

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
            socVendor = "Apple",
            socModel = cpuModel
        )
    }

    /** 通过 sysctl 读取字符串值 */
    private fun readSysctlString(name: String): String {
        return memScoped {
            val size = alloc<size_tVar>()
            sysctlbyname(name, null, size.ptr, null, 0u)
            if (size.value == 0uL) return@memScoped ""
            val buf = ByteArray(size.value.toInt())
            buf.usePinned { pinned ->
                sysctlbyname(name, pinned.addressOf(0), size.ptr, null, 0u)
            }
            buf.decodeToString().trimEnd('\u0000')
        }
    }

    /** 通过 sysctl 读取 Long 值 */
    private fun readSysctlLong(name: String): Long {
        return memScoped {
            val size = alloc<size_tVar>()
            size.value = sizeOf<kotlinx.cinterop.LongVar>().toULong()
            val value = alloc<kotlinx.cinterop.LongVar>()
            sysctlbyname(name, value.ptr, size.ptr, null, 0u)
            value.value
        }
    }

    /** 将 Apple 设备标识（如 iPhone16,2）映射为可读芯片名称 */
    private fun mapAppleChip(machine: String): String {
        return when {
            // iPhone 16 系列 → A18 Pro
            machine.startsWith("iPhone17") -> "Apple A18 Pro"
            machine.startsWith("iPhone16") -> "Apple A16"
            machine.startsWith("iPhone15") -> "Apple A16 Bionic / A15 Bionic"
            machine.startsWith("iPhone14") -> "Apple A15 Bionic"
            machine.startsWith("iPhone13") -> "Apple A14 Bionic"
            machine.startsWith("iPhone12") -> "Apple A13 Bionic"
            // iPad
            machine.startsWith("iPad13") || machine.startsWith("iPad14") -> "Apple M1 / M2"
            machine.startsWith("iPad15") -> "Apple M2 / A16"
            else -> "Apple SoC ($machine)"
        }
    }
}
