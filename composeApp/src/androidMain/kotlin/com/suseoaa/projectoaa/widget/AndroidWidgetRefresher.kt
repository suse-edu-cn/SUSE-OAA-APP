package com.suseoaa.projectoaa.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.suseoaa.projectoaa.composeapp.widget.RecentExamsWidget

/**
 * Android 实现：调用 Glance 的 updateAll() 刷新桌面考试小组件。
 */
class AndroidWidgetRefresher(private val context: Context) : WidgetRefresher {
    override suspend fun refreshExamWidgets() {
        RecentExamsWidget().updateAll(context)
    }
}
