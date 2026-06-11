package com.suseoaa.projectoaa.widget

/**
 * iOS 实现：空操作（iOS 小组件通过独立的 WidgetKit extension 刷新，无需此处处理）。
 */
class IosWidgetRefresher : WidgetRefresher {
    override suspend fun refreshExamWidgets() {
        // no-op on iOS
    }
}
