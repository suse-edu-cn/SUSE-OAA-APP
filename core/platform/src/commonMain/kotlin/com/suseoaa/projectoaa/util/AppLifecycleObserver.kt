package com.suseoaa.projectoaa.util

/**
 * 平台生命周期监听器
 * 用于检测 App 前后台切换，驱动定时签到调度器
 */
expect class AppLifecycleObserver {
    fun startObserving(onForeground: () -> Unit, onBackground: () -> Unit)
    fun stopObserving()
}
