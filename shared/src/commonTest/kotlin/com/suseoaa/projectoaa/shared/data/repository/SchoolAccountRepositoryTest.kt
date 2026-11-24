package com.suseoaa.projectoaa.shared.data.repository

import com.suseoaa.projectoaa.shared.data.local.store.SemesterStore
import com.suseoaa.projectoaa.shared.data.local.store.SessionStore
import com.suseoaa.projectoaa.shared.data.remote.InMemoryPreferences
import com.suseoaa.projectoaa.shared.domain.model.course.ClassTimeEntity
import com.suseoaa.projectoaa.shared.domain.model.course.CourseAccountEntity
import com.suseoaa.projectoaa.shared.domain.model.course.CourseEntity
import com.suseoaa.projectoaa.shared.domain.model.course.CourseWithTimes
import com.suseoaa.projectoaa.shared.domain.model.course.PracticeCourseEntity
import com.suseoaa.projectoaa.shared.domain.model.course.SemesterCalendarInfo
import com.suseoaa.projectoaa.shared.domain.model.school.CourseResponseJson
import com.suseoaa.projectoaa.shared.domain.model.school.StudentInfo
import com.suseoaa.projectoaa.shared.domain.repository.LocalCourseRepository
import com.suseoaa.projectoaa.shared.domain.repository.SchoolAuthRepository
import com.suseoaa.projectoaa.shared.domain.repository.SchoolCourseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SchoolAccountRepositoryTest {

    private class FakeLocalCourseRepository : LocalCourseRepository {
        val accountsFlow = MutableStateFlow<List<CourseAccountEntity>>(emptyList())

        override fun getAllAccounts(): Flow<List<CourseAccountEntity>> = accountsFlow

        override suspend fun getAccountById(studentId: String): CourseAccountEntity? =
            accountsFlow.value.firstOrNull { it.studentId == studentId }

        override suspend fun insertOrReplaceAccount(account: CourseAccountEntity) {
            val next = accountsFlow.value.filterNot { it.studentId == account.studentId } + account
            accountsFlow.value = next.sortedBy { it.sortIndex }
        }

        override suspend fun updateSortIndex(studentId: String, newIndex: Int) {}
        override suspend fun getMaxSortIndex(): Int = accountsFlow.value.maxOfOrNull { it.sortIndex } ?: 0
        override suspend fun deleteAccount(studentId: String) {
            accountsFlow.value = accountsFlow.value.filterNot { it.studentId == studentId }
        }
        override suspend fun updateMajorInfo(studentId: String, jgId: String, zyhId: String, njdmId: String) {}
        override suspend fun updateAllSortIndices(accounts: List<CourseAccountEntity>) {}
        override fun getCoursesByTerm(studentId: String, xnm: String, xqm: String): Flow<List<CourseEntity>> = flowOf(emptyList())
        override fun getClassTimesByTerm(studentId: String, xnm: String, xqm: String): Flow<List<ClassTimeEntity>> = flowOf(emptyList())
        override fun getCourses(studentId: String, xnm: String, xqm: String): Flow<List<CourseWithTimes>> = flowOf(emptyList())
        override fun getAllCoursesByStudent(studentId: String): Flow<List<CourseWithTimes>> = flowOf(emptyList())
        override fun getAvailableTerms(studentId: String): Flow<List<Pair<String, String>>> = flowOf(emptyList())
        override suspend fun insertCourse(course: CourseEntity) {}
        override suspend fun insertClassTime(time: ClassTimeEntity) {}
        override suspend fun deleteRemoteCoursesByTerm(studentId: String, xnm: String, xqm: String) {}
        override suspend fun deleteAllCoursesByStudent(studentId: String) {}
        override suspend fun deleteCourse(studentId: String, courseName: String, xnm: String, xqm: String, isCustom: Boolean) {}
        override suspend fun updateTermCourses(studentId: String, xnm: String, xqm: String, courses: List<CourseEntity>, times: List<ClassTimeEntity>, practiceCourses: List<PracticeCourseEntity>) {}
        override fun getPracticeCourses(studentId: String, xnm: String, xqm: String): Flow<List<PracticeCourseEntity>> = flowOf(emptyList())
        override suspend fun insertCustomCourse(course: CourseEntity, time: ClassTimeEntity) {}

        override suspend fun saveFromResponse(studentId: String, password: String, response: CourseResponseJson) {
            val xsxx = response.xsxx
            insertOrReplaceAccount(
                CourseAccountEntity(
                    studentId = studentId,
                    password = password,
                    name = xsxx?.name ?: "用户$studentId",
                    className = xsxx?.className ?: "",
                    njdmId = xsxx?.njdmId ?: "2024",
                    major = xsxx?.major ?: "",
                    sortIndex = getMaxSortIndex() + 1,
                    jgId = "",
                    zyhId = xsxx?.zyhId ?: ""
                )
            )
        }

        override suspend fun addCustomCourse(studentId: String, xnm: String, xqm: String, courseName: String, location: String, teacher: String, weeks: String, dayOfWeek: Int, startNode: Int, duration: Int) {}
    }

    private class FakeSchoolAuthRepository(
        var nextResult: Result<String> = Result.success("ok")
    ) : SchoolAuthRepository {
        var invalidateCount = 0
        override fun invalidateSession() {
            invalidateCount++
        }

        override suspend fun login(username: String, password: String): Result<String> = nextResult
    }

    private class FakeSchoolCourseRepository(
        var scheduleResult: Result<CourseResponseJson> = Result.success(
            CourseResponseJson(
                xsxx = StudentInfo(name = "张三", className = "计科241", njdmId = "2024")
            )
        ),
        var calendarInfo: SemesterCalendarInfo? = SemesterCalendarInfo("2025-09-01", hasWeekZero = false)
    ) : SchoolCourseRepository {
        override suspend fun getCourseSchedule(year: String, semester: String): Result<CourseResponseJson> = scheduleResult
        override suspend fun fetchSemesterStart(): SemesterCalendarInfo? = calendarInfo
    }

    @Test
    fun firstAccountIsAutomaticallySetAsDefaultWhenCurrentStudentIdIsEmpty() = runTest {
        val localRepo = FakeLocalCourseRepository()
        val authRepo = FakeSchoolAuthRepository()
        val courseRepo = FakeSchoolCourseRepository()
        val prefs = InMemoryPreferences()
        val sessionStore = SessionStore(prefs)
        val semesterStore = SemesterStore(prefs)
        val repo = SchoolAccountRepositoryImpl(localRepo, authRepo, courseRepo, sessionStore, semesterStore)

        assertFalse(repo.hasValidDefaultAccount())

        val added = repo.addSchoolAccount("20240001", "jwgl-pwd").getOrThrow()

        assertTrue(added.isSetAsDefault)
        assertTrue(added.scheduleImported)
        assertEquals("张三", added.account.name)
        assertEquals("20240001", sessionStore.currentStudentId.first())
        assertTrue(repo.hasValidDefaultAccount())
        assertEquals("20240001", repo.getCurrentAccount()?.studentId)
        assertEquals("2025-09-01", semesterStore.getSemesterStartDate())
    }

    @Test
    fun setsNewAccountAsDefaultWhenSessionHasOaaStudentIdButNoSavedSchoolAccount() = runTest {
        val localRepo = FakeLocalCourseRepository()
        val authRepo = FakeSchoolAuthRepository()
        val courseRepo = FakeSchoolCourseRepository()
        val prefs = InMemoryPreferences()
        val sessionStore = SessionStore(prefs)
        // 模拟 OAA 登录时写入了 currentStudentId，但本地教务账号表为空
        sessionStore.saveCurrentStudentId("20249999")
        val repo = SchoolAccountRepositoryImpl(localRepo, authRepo, courseRepo, sessionStore, SemesterStore(prefs))

        assertFalse(repo.hasValidDefaultAccount(), "数据库无对应教务账号时不应视为已有默认教务账号")
        assertNull(repo.getCurrentAccount())

        val added = repo.addSchoolAccount("20240002", "pwd").getOrThrow()

        assertTrue(added.isSetAsDefault)
        assertEquals("20240002", sessionStore.currentStudentId.first())
        assertTrue(repo.hasValidDefaultAccount())
    }

    @Test
    fun addingSecondAccountPreservesExistingDefaultAccount() = runTest {
        val localRepo = FakeLocalCourseRepository()
        val authRepo = FakeSchoolAuthRepository()
        val courseRepo = FakeSchoolCourseRepository()
        val prefs = InMemoryPreferences()
        val sessionStore = SessionStore(prefs)
        val repo = SchoolAccountRepositoryImpl(localRepo, authRepo, courseRepo, sessionStore, SemesterStore(prefs))

        repo.addSchoolAccount("20240001", "pwd1").getOrThrow()
        val second = repo.addSchoolAccount("20240002", "pwd2").getOrThrow()

        assertFalse(second.isSetAsDefault)
        assertEquals("20240001", sessionStore.currentStudentId.first())
        assertEquals(2, localRepo.accountsFlow.value.size)
        assertEquals("20240001", repo.getCurrentAccount()?.studentId)
    }

    @Test
    fun scheduleFailureStillSavesVerifiedAccountAndSetsDefaultWhenNoneExists() = runTest {
        val localRepo = FakeLocalCourseRepository()
        val authRepo = FakeSchoolAuthRepository()
        val courseRepo = FakeSchoolCourseRepository(
            scheduleResult = Result.failure(IllegalStateException("课表接口维护中"))
        )
        val prefs = InMemoryPreferences()
        val sessionStore = SessionStore(prefs)
        val repo = SchoolAccountRepositoryImpl(localRepo, authRepo, courseRepo, sessionStore, SemesterStore(prefs))

        val added = repo.addSchoolAccount("20240003", "pwd3").getOrThrow()

        assertTrue(added.isSetAsDefault)
        assertFalse(added.scheduleImported)
        assertEquals("20240003", sessionStore.currentStudentId.first())
        assertEquals("20240003", localRepo.getAccountById("20240003")?.studentId)
    }

    @Test
    fun failedLoginDoesNotSaveAccountOrChangeDefault() = runTest {
        val localRepo = FakeLocalCourseRepository()
        val authRepo = FakeSchoolAuthRepository(Result.failure(IllegalArgumentException("用户名或密码错误")))
        val courseRepo = FakeSchoolCourseRepository()
        val prefs = InMemoryPreferences()
        val sessionStore = SessionStore(prefs)
        val repo = SchoolAccountRepositoryImpl(localRepo, authRepo, courseRepo, sessionStore, SemesterStore(prefs))

        val result = repo.addSchoolAccount("20240004", "wrong")

        assertTrue(result.isFailure)
        assertTrue(localRepo.accountsFlow.value.isEmpty())
        assertNull(sessionStore.currentStudentId.first())
        assertEquals(1, authRepo.invalidateCount)
    }
}
