package com.suseoaa.projectoaa.shared.domain.nearfield

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Android 平台的近场发现管理器实现
 * 使用 Android 原生的网络服务发现（Network Service Discovery, NSD）机制
 */
actual class NearFieldDiscoveryManager actual constructor() : KoinComponent {
    /**
     * Android上下文，通过Koin注入
     */
    private val androidContext: Context by inject()

    /**
     * Android NSD管理器实例
     */
    private val nsdManager: NsdManager by lazy {
        androidContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    /**
     * 服务类型标识符，用于过滤特定的签到任务
     */
    private val SERVICE_TYPE = "_oaa_checkin._udp"

    private val _discoveredTasks = MutableStateFlow<List<NearFieldCheckinTask>>(emptyList())
    actual val discoveredTasks: StateFlow<List<NearFieldCheckinTask>> = _discoveredTasks.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    actual val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _isBroadcasting = MutableStateFlow(false)
    actual val isBroadcasting: StateFlow<Boolean> = _isBroadcasting.asStateFlow()

    /**
     * 当前正在运行的扫描监听器
     */
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    /**
     * 当前正在运行的广播监听器
     */
    private var registrationListener: NsdManager.RegistrationListener? = null

    actual fun startScanning() {
        if (_isScanning.value) return

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                _isScanning.value = true
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                // 发现服务后，需要进行解析以获取详细信息
                if (service.serviceType == SERVICE_TYPE || service.serviceType == "$SERVICE_TYPE.") {
                    nsdManager.resolveService(service, createResolveListener())
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                // 服务丢失时，从列表中移除
                val currentList = _discoveredTasks.value.toMutableList()
                currentList.removeAll { it.taskIdentifier == service.serviceName }
                _discoveredTasks.value = currentList
            }

            override fun onDiscoveryStopped(serviceType: String) {
                _isScanning.value = false
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                _isScanning.value = false
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                _isScanning.value = false
            }
        }

        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    actual fun stopScanning() {
        discoveryListener?.let {
            nsdManager.stopServiceDiscovery(it)
            discoveryListener = null
        }
    }

    actual fun startBroadcasting(task: NearFieldCheckinTask) {
        if (_isBroadcasting.value) return

        val serviceInfo = NsdServiceInfo().apply {
            // 使用任务标识符作为服务名称
            serviceName = task.taskIdentifier
            serviceType = SERVICE_TYPE
            // 使用任务中定义的实际 TCP 服务器端口
            port = task.hostPort ?: 8888
            // 在属性中存储活动名称和其他信息
            setAttribute("activityName", task.activityName)
            setAttribute("hostName", task.hostDeviceName)
            setAttribute("publishTimestamp", task.publishTimestamp.toString())
            setAttribute("startTime", task.startTime.toString())
            setAttribute("endTime", task.endTime.toString())
            setAttribute("nonce", task.securityNonce)
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
                _isBroadcasting.value = true
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                _isBroadcasting.value = false
            }

            override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                _isBroadcasting.value = false
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                _isBroadcasting.value = false
            }
        }

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    actual fun stopBroadcasting() {
        registrationListener?.let {
            nsdManager.unregisterService(it)
            registrationListener = null
        }
    }

    actual fun release() {
        stopScanning()
        stopBroadcasting()
    }

    /**
     * 创建服务解析监听器
     * 当NSD发现一个潜在服务时，调用此监听器获取其详细的TXT记录或端口信息
     */
    private fun createResolveListener() = object : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            // 解析失败处理
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            val activityName = serviceInfo.attributes["activityName"]?.decodeToString() ?: "未知活动"
            val hostName = serviceInfo.attributes["hostName"]?.decodeToString() ?: "未知主机"
            val publishTimestamp = serviceInfo.attributes["publishTimestamp"]?.decodeToString()?.toLongOrNull() ?: System.currentTimeMillis()
            val startTime = serviceInfo.attributes["startTime"]?.decodeToString()?.toLongOrNull() ?: publishTimestamp
            val endTime = serviceInfo.attributes["endTime"]?.decodeToString()?.toLongOrNull() ?: (publishTimestamp + 3600_000)
            val nonce = serviceInfo.attributes["nonce"]?.decodeToString() ?: ""

            val task = NearFieldCheckinTask(
                taskIdentifier = serviceInfo.serviceName,
                activityName = activityName,
                hostDeviceName = hostName,
                publishTimestamp = publishTimestamp,
                securityNonce = nonce,
                startTime = startTime,
                endTime = endTime,
                hostAddress = serviceInfo.host?.hostAddress,
                hostPort = serviceInfo.port
            )

            // 更新已发现的任务列表，确保不重复
            val currentList = _discoveredTasks.value.toMutableList()
            if (currentList.none { it.taskIdentifier == task.taskIdentifier }) {
                currentList.add(task)
                _discoveredTasks.value = currentList
            }
        }
    }
}
