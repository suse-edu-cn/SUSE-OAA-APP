package com.suseoaa.projectoaa.navigation.repository
interface FeedbackRepository {
    /**
     * 提交反馈
     * @param text 反馈内容
     * @return true 表示成功, false 表示失败
     */
    suspend fun submit(text: String): Boolean
}

