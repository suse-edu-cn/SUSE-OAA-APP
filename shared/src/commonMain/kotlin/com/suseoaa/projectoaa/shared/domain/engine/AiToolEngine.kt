package com.suseoaa.projectoaa.shared.domain.engine

import com.suseoaa.projectoaa.shared.data.repository.GradeEntity

/**
 * 学业 AI 工具层兼容包装。
 *
 * 真正的意图路由、结构化查询和知识库上下文生成由 AcademicToolRouter 负责。
 * 这个类保留旧调用入口，避免 UI/ViewModel 层大范围改动。
 */
class AiToolEngine(grades: List<GradeEntity>) {
    private val router = AcademicToolRouter(grades)

    fun answerDirectlyIfPossible(query: String): String? {
        return router.route(query).directAnswer
    }

    fun buildAcademicContext(query: String = ""): String {
        return router.route(query).llmContext
    }
}
