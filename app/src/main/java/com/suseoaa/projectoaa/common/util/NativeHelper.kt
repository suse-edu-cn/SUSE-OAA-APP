package com.suseoaa.projectoaa.common.util // [关键] 包名必须是这个

/**
 * 专门用于调用 C++ 评教功能的工具类
 */
object NativeHelper {
    // 加载 C++ 库，名字对应 CMakeLists.txt 里的 add_library
    init {
        try {
            System.loadLibrary("oaa-native")
        } catch (e: UnsatisfiedLinkError) {
            e.printStackTrace()
        }
    }

    /**
     * 开始一键评教 (这是一个耗时操作)
     * @param cookie 教务系统的完整 Cookie 字符串 (如 "JSESSIONID=...; route=...")
     * @return 返回评教日志/结果
     */
    external fun startEvaluation(cookie: String): String
}