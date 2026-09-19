package com.suseoaa.projectoaa.shared.domain.repository

import com.suseoaa.projectoaa.shared.domain.model.gpa.GpaCourseWrapper
import io.ktor.client.call.*
import io.ktor.client.statement.*

/**
 * GpaRepository 的契约。
 *
 * 接口置于 domain 层、实现留在 data 层，让上层依赖抽象而非具体实现，
 * 测试中才能替换为假实现。
 */
interface GpaRepository {

    suspend fun getGpaData(studentId: String): Result<List<GpaCourseWrapper>>

    fun clearCache(): Unit
}
