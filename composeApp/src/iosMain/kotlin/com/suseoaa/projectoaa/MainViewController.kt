package com.suseoaa.projectoaa

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeUIViewController
import com.suseoaa.projectoaa.presentation.checkin.CheckinScheduler
import com.suseoaa.projectoaa.scheduling.initializeKoinIfNeeded
import com.suseoaa.projectoaa.util.AppLifecycleObserver
import org.koin.mp.KoinPlatform

@OptIn(ExperimentalComposeUiApi::class)
fun MainViewController() = ComposeUIViewController(
    configure = {
        // Compose Multiplatform 1.11.0 起默认把 parallelRendering（渲染指令编码放到独立
        // 渲染线程）打开了，官方发行说明里写了这个改动配套一个已知问题——"首帧在 Compose
        // 容器出现时可能不渲染"。这正好能解释一个现象：从导航栈跳转进入的新页面（比如
        // 成绩查询、绩点计算）底部会有一块内容没铺满，而从 App 启动就一直在渲染的首页
        // 不会有这个问题——因为只有"刚出现的 Compose 容器"才会撞上这个时序坑。
        // 这里显式关掉，退回到 1.11.0 之前的默认行为（该属性 1.11 之前默认就是 false）。
        parallelRendering = false

        initializeKoinIfNeeded()

        // 启动定时签到调度器
        try {
            val koin = KoinPlatform.getKoin()
            val scheduler = koin.get<CheckinScheduler>()
            scheduler.start()

            val observer = AppLifecycleObserver()
            observer.startObserving(
                onForeground = { scheduler.onAppForeground() },
                onBackground = { scheduler.onAppBackground() }
            )
        } catch (e: Exception) {
            println("[MainViewController] 启动定时签到调度器失败: ${e.message}")
        }
    }
) {
    App()
}
