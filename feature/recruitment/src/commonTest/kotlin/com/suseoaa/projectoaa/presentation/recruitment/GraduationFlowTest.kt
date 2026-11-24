package com.suseoaa.projectoaa.presentation.recruitment

import com.suseoaa.projectoaa.shared.domain.model.org.Department
import com.suseoaa.projectoaa.shared.domain.model.org.Role
import com.suseoaa.projectoaa.shared.domain.model.person.*
import com.suseoaa.projectoaa.shared.domain.model.recruitment.*
import com.suseoaa.projectoaa.shared.domain.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GraduationFlowTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun graduateCannotSubmitOrUploadEvenDuringEditingPeriod() = runTest(dispatcher) {
        val repo = Recruitment()
        val vm = RecruitmentViewModel(repo, Person(), Organization())
        advanceUntilIdle()
        assertEquals(TermPhase.Editing, vm.uiState.value.phase)
        assertFalse(vm.uiState.value.canEdit)
        assertFalse(vm.uiState.value.canReview)
        assertFalse(vm.uiState.value.canManageTerms)
        vm.submitApplication()
        assertEquals("已毕业成员不可填报申请表", vm.uiState.value.errorMessage)
        vm.uploadAvatar(byteArrayOf(1))
        assertEquals("已毕业成员不可填报或修改申请表", vm.uiState.value.errorMessage)
        vm.updateForm { it.copy(college = "不应修改") }
        assertEquals("", vm.uiState.value.form.college)

        repo.myApplications = listOf(Application(applicationId = 1, termId = 1, college = "原学院"))
        vm.loadInitialData()
        advanceUntilIdle()
        vm.submitApplication()
        assertEquals("已毕业成员不可修改申请表", vm.uiState.value.errorMessage)
        assertEquals("原学院", vm.uiState.value.form.college)
    }

    @Test
    fun candidateLoadAndSearchExcludeGraduatesAndAddActionRejectsThem() = runTest(dispatcher) {
        val student = UserListItem(userId = 1, name = "在校生", isGraduated = false)
        val graduate = UserListItem(userId = 2, name = "毕业生", isGraduated = true)
        val members = Members(listOf(student, graduate))
        val repo = Recruitment()
        val vm = TermManagementViewModel(repo, members)
        vm.openInterviewerManager(repo.term)
        advanceUntilIdle()
        assertEquals(listOf(1), vm.uiState.value.userResults.map { it.userId })
        assertEquals(false, members.queries.last().isGraduated)
        vm.searchUsers("毕业生")
        advanceUntilIdle()
        assertEquals(listOf(1), vm.uiState.value.userResults.map { it.userId })
        assertEquals(false, members.queries.last().isGraduated)
        vm.addInterviewers(repo.term, listOf(student, graduate), "")
        advanceUntilIdle()
        assertTrue(repo.interviewerWrites.isEmpty())
        assertEquals("存在已毕业成员，不可设置为面试官", vm.uiState.value.message)
        vm.addInterviewers(repo.term, listOf(student), "")
        advanceUntilIdle()
        assertEquals(1, repo.interviewerWrites.single().single().userId)

        members.users = listOf(student.copy(isGraduated = true), graduate)
        vm.openInterviewerManager(repo.term)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.userResults.isEmpty(), "再次打开时需要刷新毕业状态")
    }

    @Test
    fun reviewRejectsAdmissionAndPendingForGraduateAndClearsDestinationForRejection() = runTest(dispatcher) {
        val repo = Recruitment()
        val graduate = Application(applicationId = 1, userId = 1, isGraduated = true)
        repo.applications = listOf(graduate)
        val vm = RecruitmentReviewViewModel(repo, Organization())
        vm.ensureLoaded()
        advanceUntilIdle()
        // 弹窗可能持有刷新前的对象，保存时必须以当前列表状态为准。
        for (value in listOf("录取第一志愿", "待定")) {
            vm.saveDecision(graduate.copy(isGraduated = false), InterviewDecision(value, 1, 2, ""))
            advanceUntilIdle()
            assertTrue(repo.resultWrites.isEmpty())
        }
        vm.saveDecision(graduate, InterviewDecision("淘汰", 1, 2, "已毕业"))
        advanceUntilIdle()
        assertEquals(InterviewDecision("淘汰", 0, 0, "已毕业"), repo.resultWrites.single())
    }

    @Test
    fun studentMayStillReceiveAdmission() = runTest(dispatcher) {
        val repo = Recruitment()
        val student = Application(applicationId = 1, userId = 1, isGraduated = false)
        repo.applications = listOf(student)
        val vm = RecruitmentReviewViewModel(repo, Organization())
        vm.ensureLoaded()
        advanceUntilIdle()
        val decision = InterviewDecision("录取第一志愿", 1, 2, "通过")
        vm.saveDecision(student, decision)
        advanceUntilIdle()
        assertEquals(decision, repo.resultWrites.single())
        val state = RecruitmentUiState(phase = TermPhase.Editing, currentUser = CurrentUser(PersonData(isGraduated = false), 80, 1, 1))
        assertTrue(state.canEdit)
        assertTrue(state.canReview)
        assertTrue(state.canManageTerms)
    }

    @Test
    fun reviewFiltersAndExportForwardParametersAndBuildBitmask() = runTest(dispatcher) {
        val repo = Recruitment()
        repo.applications = listOf(Application(applicationId = 9, termId = 1, name = "张三"))
        val vm = RecruitmentReviewViewModel(repo, Organization())
        vm.ensureLoaded()
        advanceUntilIdle()

        vm.selectHasResult(false)
        advanceUntilIdle()
        assertEquals(false, repo.applicationQueries.last().third)

        vm.updateNameFilter("张三")
        vm.searchByName()
        advanceUntilIdle()
        assertEquals("张三", repo.applicationQueries.last().fourth)

        vm.loadExportFields()
        advanceUntilIdle()
        assertEquals(22, vm.uiState.value.exportFieldsConfig?.totalFields)

        vm.exportResults(setOf(0, 2, 21))
        advanceUntilIdle()
        assertEquals("1010000000000000000001", repo.exportQueries.single().fourth)
        assertEquals("result.xlsx", vm.uiState.value.exportedFile?.fileName)
        vm.consumeExportedFile()
        assertEquals(null, vm.uiState.value.exportedFile)
    }

    private data class AppQueryRecord(val termId: Int, val departmentId: Int?, val third: Boolean?, val fourth: String?)

    private class Recruitment : RecruitmentRepository {
        val term = Term(termId = 1, editPeriod = Period("2001-01-01", "9998-01-01"), queryPeriod = Period("9998-01-02", "9999-01-01"))
        var myApplications = emptyList<Application>()
        var applications = emptyList<Application>()
        val applicationQueries = mutableListOf<AppQueryRecord>()
        val exportQueries = mutableListOf<AppQueryRecord>()
        val interviewerWrites = mutableListOf<List<InterviewerEntry>>()
        val resultWrites = mutableListOf<InterviewDecision>()
        override suspend fun getTerms(year: Int?, type: String?) = Result.success(listOf(term))
        override suspend fun getMyApplications() = Result.success(myApplications)
        override suspend fun getApplications(
            termId: Int,
            departmentId: Int?,
            hasResult: Boolean?,
            name: String?
        ): Result<List<Application>> {
            applicationQueries += AppQueryRecord(termId, departmentId, hasResult, name)
            return Result.success(applications)
        }
        override suspend fun getDecisions() = Result.success(listOf("录取第一志愿", "待定", "淘汰"))
        override suspend fun getInterviewResults(termId: Int) = Result.success(emptyList<InterviewResult>())
        override suspend fun getInterviewers(termId: Int) = Result.success(emptyList<Interviewer>())
        override suspend fun getFillableRoles(departmentId: Int) = Result.success(emptyList<Role>())
        override suspend fun getFillableDepartments(roleId: Int) = Result.success(emptyList<Department>())
        override suspend fun createInterviewers(termId: Int, interviewers: List<InterviewerEntry>): Result<Unit> {
            interviewerWrites += interviewers
            return Result.success(Unit)
        }
        override suspend fun saveInterviewResult(applicationId: Int, existingResultId: Int?, decision: InterviewDecision): Result<Unit> {
            resultWrites += decision
            return Result.success(Unit)
        }
        override suspend fun getExportFields(): Result<ExportFieldsConfig> = Result.success(
            ExportFieldsConfig(
                totalFields = 22,
                allBits = "1".repeat(22),
                fields = listOf(
                    ExportFieldOption(0, "业务周期"),
                    ExportFieldOption(2, "学号"),
                    ExportFieldOption(21, "更新时间")
                )
            )
        )
        override suspend fun exportInterviewResults(
            termId: Int,
            departmentId: Int?,
            hasResult: Boolean?,
            fieldsBits: String?
        ): Result<ExportedSpreadsheet> {
            exportQueries += AppQueryRecord(termId, departmentId, hasResult, fieldsBits)
            return Result.success(
                ExportedSpreadsheet(
                    fileName = "result.xlsx",
                    mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    bytes = byteArrayOf(0x50, 0x4B, 0x03, 0x04)
                )
            )
        }
        override suspend fun createTerm(year: Int, type: String, title: String, editPeriod: Period, queryPeriod: Period): Result<Unit> = error("unused")
        override suspend fun updateTerm(termId: Int, title: String, editPeriod: Period, queryPeriod: Period): Result<Unit> = error("unused")
        override suspend fun deleteTerm(termId: Int): Result<Unit> = error("unused")
        override suspend fun uploadApplicationAvatar(imageData: ByteArray): Result<StoredFile> = error("毕业成员不应上传照片")
        override suspend fun createApplication(termId: Int, form: ApplicationForm): Result<Unit> = error("毕业成员不应提交")
        override suspend fun updateApplication(termId: Int, existing: Application, form: ApplicationForm): Result<Unit> = error("毕业成员不应修改")
        override suspend fun deleteApplication(applicationId: Int): Result<Unit> = error("unused")
        override suspend fun updateInterviewer(interviewerId: Int, remark: String): Result<Unit> = error("unused")
        override suspend fun deleteInterviewer(interviewerId: Int): Result<Unit> = error("unused")
    }

    private class Members(var users: List<UserListItem>) : UserManagementRepository {
        val queries = mutableListOf<UserQuery>()
        override suspend fun listUsers(query: UserQuery): Result<UserPage> {
            queries += query
            return Result.success(UserPage(users.size, users))
        }
        override suspend fun batchUpdate(items: List<UserBatchItem>): Result<List<UserBatchFailure>> = error("unused")
        override suspend fun deleteUser(userId: Int): Result<Unit> = error("unused")
    }

    private class Organization : OrganizationRepository {
        override suspend fun getDepartments(forceRefresh: Boolean) = Result.success(emptyList<Department>())
        override suspend fun getRoles(forceRefresh: Boolean) = Result.success(emptyList<Role>())
        override suspend fun createDepartment(name: String, type: String): Result<Unit> = error("unused")
        override suspend fun updateDepartment(id: Int, name: String, type: String, isActive: Boolean): Result<Unit> = error("unused")
        override suspend fun createRole(name: String, level: Int, type: String): Result<Unit> = error("unused")
        override suspend fun updateRole(id: Int, name: String, level: Int, type: String, isActive: Boolean): Result<Unit> = error("unused")
    }

    private class Person : PersonRepository {
        override suspend fun getCurrentUser() = Result.success(CurrentUser(PersonData(isGraduated = true), 100, 1, 1))
        override suspend fun logout(): Unit = error("unused")
        override suspend fun getPersonInfo(): Result<PersonData> = error("unused")
        override suspend fun updateProfile(username: String, email: String, grade: Int?): Result<String> = error("unused")
        override suspend fun uploadAvatar(imageData: ByteArray): Result<String> = error("unused")
        override suspend fun uploadFile(bytes: ByteArray, fileName: String, mimeType: String, scene: String): Result<StoredFile> = error("unused")
        override suspend fun sendDeleteAccountCode(): Result<Unit> = error("unused")
        override suspend fun deleteAccount(code: String): Result<Unit> = error("unused")
        override suspend fun sendCancelDeleteCode(): Result<Unit> = error("unused")
        override suspend fun cancelDeleteAccount(code: String): Result<Unit> = error("unused")
        override suspend fun checkServerHealth(): Result<ServerHealthStatus> = error("unused")
    }
}
