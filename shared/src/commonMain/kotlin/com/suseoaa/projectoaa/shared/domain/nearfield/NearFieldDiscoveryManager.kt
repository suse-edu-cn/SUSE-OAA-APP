package com.suseoaa.projectoaa.shared.domain.nearfield

import kotlinx.coroutines.flow.StateFlow

/**
 * 近场发现管理器的跨平台接口定义
 * 负责处理基于局域网或蓝牙的设备广播与服务发现
 */
expect class NearFieldDiscoveryManager() {
    /**
     * 当前发现的可用签到任务列表的流
     */
    val discoveredTasks: StateFlow<List<NearFieldCheckinTask>>

    /**
     * 当前扫描状态的流
     */
    val isScanning: StateFlow<Boolean>

    /**
     * 当前广播状态的流
     */
    val isBroadcasting: StateFlow<Boolean>

    /**
     * 开始扫描附近的签到任务
     */
    fun startScanning()

    /**
     * 停止扫描
     */
    fun stopScanning()

    /**
     * 开始广播一个签到任务（作为发布端）
     *
     * @param task 需要广播的任务信息
     */
    fun startBroadcasting(task: NearFieldCheckinTask)

    /**
     * 停止广播
     */
    fun stopBroadcasting()
    
    /**
     * 清理资源，关闭管理器
     */
    fun release()
}
