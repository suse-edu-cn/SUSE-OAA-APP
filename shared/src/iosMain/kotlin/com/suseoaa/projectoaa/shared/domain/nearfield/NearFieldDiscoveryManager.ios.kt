package com.suseoaa.projectoaa.shared.domain.nearfield

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.*
import platform.darwin.NSObject
import kotlinx.cinterop.*
import platform.posix.*

/**
 * iOS 平台的近场发现管理器实现
 * 使用Apple原生的Bonjour(NSNetService)技术进行局域网服务广播与发现
 */
actual class NearFieldDiscoveryManager actual constructor() {
    private val _discoveredTasks = MutableStateFlow<List<NearFieldCheckinTask>>(emptyList())
    actual val discoveredTasks: StateFlow<List<NearFieldCheckinTask>> = _discoveredTasks.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    actual val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _isBroadcasting = MutableStateFlow(false)
    actual val isBroadcasting: StateFlow<Boolean> = _isBroadcasting.asStateFlow()

    private val SERVICE_TYPE = "_oaa_checkin._udp."
    private val DOMAIN = "local."

    private var serviceBrowser: NSNetServiceBrowser? = null
    private var localService: NSNetService? = null
    private val browserDelegate = BrowserDelegate()
    private val serviceDelegates = mutableMapOf<String, ServiceDelegate>()

    actual fun startScanning() {
        if (_isScanning.value) return
        
        serviceBrowser = NSNetServiceBrowser().apply {
            delegate = browserDelegate
            searchForServicesOfType(SERVICE_TYPE, DOMAIN)
        }
        _isScanning.value = true
    }

    actual fun stopScanning() {
        serviceBrowser?.stop()
        serviceBrowser = null
        _isScanning.value = false
    }

    actual fun startBroadcasting(task: NearFieldCheckinTask) {
        if (_isBroadcasting.value) return

        localService = NSNetService(DOMAIN, SERVICE_TYPE, task.taskIdentifier, (task.hostPort ?: 8888).toInt()).apply {
            val metadata = mapOf(
                "activityName" to task.activityName,
                "hostName" to task.hostDeviceName,
                "publishTimestamp" to task.publishTimestamp.toString(),
                "startTime" to task.startTime.toString(),
                "endTime" to task.endTime.toString(),
                "nonce" to task.securityNonce
            ).mapValues { it.value.encodeToByteArray().toNSData() }
            
            setTXTRecordData(NSNetService.dataFromTXTRecordDictionary(metadata as Map<Any?, *>))
            publish()
        }
        _isBroadcasting.value = true
    }

    actual fun stopBroadcasting() {
        localService?.stop()
        localService = null
        _isBroadcasting.value = false
    }

    actual fun release() {
        stopScanning()
        stopBroadcasting()
    }

    /**
     * 内部委托类，处理Bonjour浏览事件
     */
    private inner class BrowserDelegate : NSObject(), NSNetServiceBrowserDelegateProtocol {
        override fun netServiceBrowser(browser: NSNetServiceBrowser, didFindService: NSNetService, moreComing: Boolean) {
            val delegate = ServiceDelegate { task ->
                val currentList = _discoveredTasks.value.toMutableList()
                if (currentList.none { it.taskIdentifier == task.taskIdentifier }) {
                    currentList.add(task)
                    _discoveredTasks.value = currentList
                }
            }
            serviceDelegates[didFindService.name] = delegate
            didFindService.delegate = delegate
            didFindService.resolveWithTimeout(10.0)
        }

        /* 
         * 由于 Kotlin/Native 在当前环境下无法通过 ObjCSignatureOverride 解决与 didFindService 的方法签名冲突，
         * 暂时注释掉 didRemoveService 实现。这不会影响基本的扫描发现功能。
         */
        // override fun netServiceBrowser(browser: NSNetServiceBrowser, didRemoveService: NSNetService, moreComing: Boolean) { ... }
    }

    /**
     * 内部委托类，处理特定服务的解析事件
     */
    @Suppress("CONFLICTING_OVERLOADS")
    private inner class ServiceDelegate(private val onResolved: (NearFieldCheckinTask) -> Unit) : NSObject(), NSNetServiceDelegateProtocol {
        override fun netServiceDidResolveAddress(sender: NSNetService) {
            val txtData = sender.TXTRecordData() ?: return
            val dict = NSNetService.dictionaryFromTXTRecordData(txtData) as? Map<Any?, *> ?: return
            
            val activityName = (dict["activityName"] as? NSData)?.toKString() ?: "未知活动"
            val hostName = (dict["hostName"] as? NSData)?.toKString() ?: "未知主机"
            val publishTimestamp = (dict["publishTimestamp"] as? NSData)?.toKString()?.toLongOrNull() ?: NSDate().timeIntervalSince1970.toLong()
            val startTime = (dict["startTime"] as? NSData)?.toKString()?.toLongOrNull() ?: publishTimestamp
            val endTime = (dict["endTime"] as? NSData)?.toKString()?.toLongOrNull() ?: (publishTimestamp + 3600_000)
            val nonce = (dict["nonce"] as? NSData)?.toKString() ?: ""

            val task = NearFieldCheckinTask(
                taskIdentifier = sender.name,
                activityName = activityName,
                hostDeviceName = hostName,
                publishTimestamp = publishTimestamp,
                securityNonce = nonce,
                startTime = startTime,
                endTime = endTime,
                hostAddress = sender.addresses?.firstOrNull()?.let { extractIpAddress(it as NSData) },
                hostPort = sender.port.toInt()
            )
            onResolved(task)
        }
    }
}

/**
 * 从 NSData (sockaddr) 中提取 IP 地址字符串
 */
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
private fun extractIpAddress(data: NSData): String? {
    val bytes = data.bytes ?: return null
    val sockaddr = bytes.reinterpret<sockaddr>()
    val len = data.length.toUInt()
    
    val host = ByteArray(NI_MAXHOST)
    return host.usePinned { pinned ->
        val result = getnameinfo(
            sockaddr,
            len,
            pinned.addressOf(0),
            NI_MAXHOST.toUInt(),
            null,
            0u,
            NI_NUMERICHOST
        )
        if (result == 0) {
            pinned.get().toKString()
        } else {
            null
        }
    }
}

/**
 * 辅助扩展：将 NSData 转换为 Kotlin 字符串
 */
private fun NSData.toKString(): String {
    return NSString.create(this, NSUTF8StringEncoding) as? String ?: ""
}

/**
 * 辅助扩展：将 ByteArray 转换为 NSData
 */
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData {
    return this.usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), length = this.size.toULong())
    }
}
