package com.suseoaa.projectoaa.shared.domain.repository

import com.suseoaa.projectoaa.shared.domain.model.course.CourseAccountEntity
import kotlinx.coroutines.flow.Flow

/**
 * 添加/验证教务系统账号后的结果。
 */
data class AddedSchoolAccount(
    val account: CourseAccountEntity,
    val isSetAsDefault: Boolean,
    val scheduleImported: Boolean
)

/**
 * 教务系统账号仓库。
 *
 * 统一封装「当前默认教务账号识别」与「登录并保存教务账号」，
 * 确保没有默认教务账号时自动将新添加账号设为默认。
 */
interface SchoolAccountRepository {
    fun observeCurrentAccount(): Flow<CourseAccountEntity?>

    fun observeHasDefaultAccount(): Flow<Boolean>

    suspend fun getCurrentAccount(): CourseAccountEntity?

    suspend fun hasValidDefaultAccount(): Boolean

    suspend fun addSchoolAccount(
        studentId: String,
        password: String,
        xnm: String? = null,
        xqm: String? = null,
        forceSetDefault: Boolean = false
    ): Result<AddedSchoolAccount>
}
