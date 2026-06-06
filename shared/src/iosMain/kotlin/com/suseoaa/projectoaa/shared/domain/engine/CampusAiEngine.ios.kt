package com.suseoaa.projectoaa.shared.domain.engine

import kotlinx.coroutines.delay

actual object CampusAiEngine {
    actual fun initContext(context: Any) {
        // iOS does not need a context
    }

    actual fun isModelAvailable(): Boolean {
        // TODO: 检查本地模型文件是否存在
        return false
    }

    actual suspend fun loadModel(): Boolean {
        // TODO: 初始化 iOS 端的 LiteRT C++ 引擎
        delay(500)
        return true
    }

    actual suspend fun unloadModel() {
        // TODO: 释放 iOS 引擎资源
    }

    actual suspend fun summarizeAcademicMessage(content: String): String {
        // TODO: iOS 端模型推理
        delay(1500)
        return "[AI 摘要] iOS 端暂未接入推理引擎。原文：${content.take(20)}..."
    }

    actual suspend fun chatWithContext(context: String, query: String): String {
        // TODO: iOS 端模型推理
        delay(2000)
        return "这是来自 iOS 本地 AI 的模拟回答。\n你问的是：$query\n根据上下文数据，你的 GPA 不错，请继续保持！"
    }
}
