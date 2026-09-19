package com.suseoaa.projectoaa.widget

/**
 * 平台抽象：在应用数据变更后触发桌面小组件刷新。
 * Android 端调用 Glance 的 updateAll()；iOS 端为空操作。
 */
interface WidgetRefresher {
    /** 刷新所有考试相关的小组件 */
    suspend fun refreshExamWidgets()
}
