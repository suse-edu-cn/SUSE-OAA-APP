package com.suseoaa.projectoaa.ui.component

import androidx.compose.runtime.compositionLocalOf

/**
 * 标记当前 Main Tab 页面是否处于前台可见状态。
 * 保活页面可通过该信号在切回前台时主动执行刷新逻辑。
 */
val LocalMainTabVisible = compositionLocalOf { true }