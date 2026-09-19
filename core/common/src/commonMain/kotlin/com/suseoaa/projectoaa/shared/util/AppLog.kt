package com.suseoaa.projectoaa.shared.util

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

/**
 * 全项目统一日志入口。
 *
 * 之前各处直接用 `println()` 打日志，Release 包里同样会输出、无法按等级过滤，
 * 也拿不到 tag 和堆栈。这里用 Napier 收口：debug 级日志只在 [init] 传入
 * `debug = true` 时才装载 Antilog，Release 包默认不输出。
 *
 * Napier 本身在 shared 里是 `implementation` 依赖，不会泄漏给上层模块，
 * 上层只依赖 AppLog 这一层字符串 API。
 */
object AppLog {

    private var initialized = false

    /**
     * 在应用启动时调用一次（Android 在 Application、iOS 在 Koin 初始化处）。
     *
     * @param debug 为 false 时不装载任何 Antilog，所有日志调用变成空操作。
     */
    fun init(debug: Boolean) {
        if (initialized) return
        initialized = true
        if (debug) {
            Napier.base(DebugAntilog())
        }
    }

    fun v(message: String, tag: String? = null, throwable: Throwable? = null) =
        Napier.v(message = message, throwable = throwable, tag = tag)

    fun d(message: String, tag: String? = null, throwable: Throwable? = null) =
        Napier.d(message = message, throwable = throwable, tag = tag)

    fun i(message: String, tag: String? = null, throwable: Throwable? = null) =
        Napier.i(message = message, throwable = throwable, tag = tag)

    fun w(message: String, tag: String? = null, throwable: Throwable? = null) =
        Napier.w(message = message, throwable = throwable, tag = tag)

    fun e(message: String, tag: String? = null, throwable: Throwable? = null) =
        Napier.e(message = message, throwable = throwable, tag = tag)
}
