package com.suseoaa.projectoaa.shared.domain.engine

/**
 * 校园本地 AI 引擎接口
 * 负责与底层模型（LiteRT / MediaPipe 等）进行交互
 */
expect object CampusAiEngine {
    /**
     * 初始化引擎上下文（如果在 Android 环境，请传入 ApplicationContext）
     */
    fun initContext(context: Any)

    /**
     * 判断模型文件是否已下载并准备就绪
     */
    fun isModelAvailable(): Boolean

    /**
     * 载入模型到内存（如果在后台调用，确保在 Dispatchers.Default 执行）
     * 如果已载入则无副作用
     */
    suspend fun loadModel(): Boolean

    /**
     * 将模型从内存中释放
     */
    suspend fun unloadModel()

    /**
     * 针对调课通知进行一句话摘要
     * @param content 原始长通知内容
     * @return 精简后的摘要（如："张三老师的《高数》5月12日调至A4-201"）
     */
    suspend fun summarizeAcademicMessage(content: String): String

    /**
     * 带预计算上下文的对话
     * @param context 注入的真实计算数据
     * @param query 用户的提问
     */
    suspend fun chatWithContext(context: String, query: String): String
}
