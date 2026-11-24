package com.suseoaa.projectoaa.shared.data.repository

import com.suseoaa.projectoaa.shared.data.local.store.SemesterStore
import com.suseoaa.projectoaa.shared.data.local.store.SessionStore
import com.suseoaa.projectoaa.shared.domain.error.AppError
import com.suseoaa.projectoaa.shared.domain.error.appFailure
import com.suseoaa.projectoaa.shared.domain.model.course.CourseAccountEntity
import com.suseoaa.projectoaa.shared.domain.repository.AddedSchoolAccount
import com.suseoaa.projectoaa.shared.domain.repository.LocalCourseRepository
import com.suseoaa.projectoaa.shared.domain.repository.SchoolAccountRepository
import com.suseoaa.projectoaa.shared.domain.repository.SchoolAuthRepository
import com.suseoaa.projectoaa.shared.domain.repository.SchoolCourseRepository
import com.suseoaa.projectoaa.shared.util.AppLog
import com.suseoaa.projectoaa.shared.util.getCurrentTerm
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate

class SchoolAccountRepositoryImpl(
    private val localCourseRepository: LocalCourseRepository,
    private val schoolAuthRepository: SchoolAuthRepository,
    private val schoolCourseRepository: SchoolCourseRepository,
    private val sessionStore: SessionStore,
    private val semesterStore: SemesterStore
) : SchoolAccountRepository {

    override fun observeCurrentAccount(): Flow<CourseAccountEntity?> = combine(
        localCourseRepository.getAllAccounts(),
        sessionStore.currentStudentId
    ) { accounts, selectedId ->
        resolveAccount(accounts, selectedId)
    }

    override fun observeHasDefaultAccount(): Flow<Boolean> = combine(
        localCourseRepository.getAllAccounts(),
        sessionStore.currentStudentId
    ) { accounts, selectedId ->
        selectedId != null && accounts.any { it.studentId == selectedId }
    }

    override suspend fun getCurrentAccount(): CourseAccountEntity? {
        val accounts = localCourseRepository.getAllAccounts().first()
        val selectedId = sessionStore.currentStudentId.first()
        return resolveAccount(accounts, selectedId)
    }

    override suspend fun hasValidDefaultAccount(): Boolean {
        val selectedId = sessionStore.currentStudentId.first() ?: return false
        val accounts = localCourseRepository.getAllAccounts().first()
        return accounts.any { it.studentId == selectedId }
    }

    override suspend fun addSchoolAccount(
        studentId: String,
        password: String,
        xnm: String?,
        xqm: String?,
        forceSetDefault: Boolean
    ): Result<AddedSchoolAccount> {
        val trimmedId = studentId.trim()
        if (trimmedId.isBlank() || password.isBlank()) {
            return appFailure(AppError.Business(userMessage = "学号和密码不能为空"))
        }

        // 在写入新账号之前检查用户是否已有有效的默认教务账号
        // （协会登录时可能往 SessionStore 写过 studentId，但数据库里尚无教务密码记录）
        val hadValidDefault = hasValidDefaultAccount()

        // 强制清理旧会话，确保真正校验本次输入的学号与密码
        schoolAuthRepository.invalidateSession()
        val loginResult = schoolAuthRepository.login(trimmedId, password)
        if (loginResult.isFailure) {
            return Result.failure(
                loginResult.exceptionOrNull()
                    ?: Exception("教务系统登录失败")
            )
        }

        val (defaultXnm, defaultXqm) = getCurrentTerm()
        val targetXnm = xnm?.takeIf { it.isNotBlank() } ?: defaultXnm
        val targetXqm = xqm?.takeIf { it.isNotBlank() } ?: defaultXqm

        val courseResult = schoolCourseRepository.getCourseSchedule(targetXnm, targetXqm)
        var scheduleImported = false

        if (courseResult.isSuccess) {
            val courseData = courseResult.getOrThrow()
            localCourseRepository.saveFromResponse(trimmedId, password, courseData)
            scheduleImported = true

            try {
                val calendarInfo = schoolCourseRepository.fetchSemesterStart()
                if (calendarInfo != null) {
                    LocalDate.parse(calendarInfo.startDate)
                    semesterStore.saveSemesterStartDate(calendarInfo.startDate)
                    semesterStore.saveSemesterHasWeekZero(calendarInfo.hasWeekZero)
                }
            } catch (e: Exception) {
                AppLog.e("[SchoolAccount] 同步校历失败: ${e.message}")
            }
        } else {
            val existing = localCourseRepository.getAccountById(trimmedId)
            val fallbackEntity = CourseAccountEntity(
                studentId = trimmedId,
                password = password,
                name = existing?.name?.takeIf { it.isNotBlank() } ?: "用户$trimmedId",
                className = existing?.className ?: "",
                njdmId = existing?.njdmId?.takeIf { it.isNotBlank() } ?: targetXnm,
                major = existing?.major ?: "",
                sortIndex = existing?.sortIndex ?: (localCourseRepository.getMaxSortIndex() + 1),
                jgId = existing?.jgId ?: "",
                zyhId = existing?.zyhId ?: ""
            )
            localCourseRepository.insertOrReplaceAccount(fallbackEntity)
        }

        val shouldSetDefault = forceSetDefault || !hadValidDefault
        if (shouldSetDefault) {
            sessionStore.saveCurrentStudentId(trimmedId)
        }

        val savedAccount = localCourseRepository.getAccountById(trimmedId)
            ?: CourseAccountEntity(
                studentId = trimmedId,
                password = password,
                name = "用户$trimmedId",
                className = "",
                njdmId = targetXnm,
                major = ""
            )

        return Result.success(
            AddedSchoolAccount(
                account = savedAccount,
                isSetAsDefault = shouldSetDefault,
                scheduleImported = scheduleImported
            )
        )
    }

    private fun resolveAccount(
        accounts: List<CourseAccountEntity>,
        selectedId: String?
    ): CourseAccountEntity? {
        if (accounts.isEmpty()) return null
        if (selectedId != null) {
            accounts.find { it.studentId == selectedId }?.let { return it }
        }
        return accounts.firstOrNull()
    }
}
