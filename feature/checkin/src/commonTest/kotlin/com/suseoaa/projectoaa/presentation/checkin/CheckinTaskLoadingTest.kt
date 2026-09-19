package com.suseoaa.projectoaa.presentation.checkin

import com.suseoaa.projectoaa.domain.checkin.PasswordAutoLogin
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinAccountData
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinTask
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 任务列表加载的分支行为。
 *
 * 这条流程按登录方式分叉：扫码账号靠 Session（过期要先刷新，刷不出来就要求重新扫码），
 * 密码账号要先自动登录（失败则回落到验证码或短信二次验证）。分支多且互相影响，
 * 是 CheckinViewModel 里最难改的部分，先用测试钉住再谈拆分。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CheckinTaskLoadingTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeCheckinRepository
    private lateinit var qrRepository: FakeQrCodeCheckinRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeCheckinRepository()
        qrRepository = FakeQrCodeCheckinRepository()
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() =
        CheckinViewModel(repository, qrRepository, PasswordAutoLogin(repository))

    /** rwmc 是教务侧的“任务名称”字段 */
    private fun task(id: Long, name: String) = CheckinTask(id = id, rwmc = name)

    /** 扫码账号，Session 远期有效 */
    private fun qrAccount(valid: Boolean) = CheckinAccountData(
        id = 1, studentId = "2021001", password = "", name = "小明", loginType = 1,
        sessionToken = if (valid) "cookie" else null,
        sessionExpireTime = if (valid) "2999-01-01 00:00:00" else null,
    )

    private fun passwordAccount() = CheckinAccountData(
        id = 2, studentId = "2021002", password = "pwd", name = "小红", loginType = 0,
    )

    @Test
    fun `扫码账号 Session 有效时直接拉取任务`() = runTest(dispatcher) {
        qrRepository.tasks = Triple(listOf(task(1, "早读")), listOf(task(2, "晚自习")), emptyList())
        val vm = viewModel()
        advanceUntilIdle()

        vm.loadTasksForAccount(qrAccount(valid = true))
        advanceUntilIdle()

        val s = vm.uiState.value
        assertFalse(s.isLoadingTasks)
        assertEquals(listOf("早读"), s.pendingTasks.map { it.rwmc })
        assertEquals(listOf("晚自习"), s.completedTasks.map { it.rwmc })
        assertEquals(qrAccount(valid = true).id, s.selectedAccount?.id)
    }

    @Test
    fun `扫码账号 Session 过期但能刷新时继续拉取`() = runTest(dispatcher) {
        qrRepository.refreshSessionResult = Result.success("新cookie")
        qrRepository.tasks = Triple(listOf(task(1, "早读")), emptyList(), emptyList())
        val vm = viewModel()
        advanceUntilIdle()

        vm.loadTasksForAccount(qrAccount(valid = false))
        advanceUntilIdle()

        assertEquals(listOf("早读"), vm.uiState.value.pendingTasks.map { it.rwmc })
        assertFalse(vm.uiState.value.showReloginDialog)
    }

    @Test
    fun `扫码账号 Session 过期且刷新失败时要求重新扫码`() = runTest(dispatcher) {
        qrRepository.refreshSessionResult = Result.failure(IllegalStateException("会话已失效"))
        val vm = viewModel()
        advanceUntilIdle()

        vm.loadTasksForAccount(qrAccount(valid = false))
        advanceUntilIdle()

        val s = vm.uiState.value
        assertTrue(s.showReloginDialog)
        assertEquals(1L, s.accountNeedRelogin?.id)
        assertFalse(s.isLoadingTasks)
    }

    @Test
    fun `密码账号未登录时自动登录失败会弹出验证码对话框`() = runTest(dispatcher) {
        // 默认 autoLoginResult 为 success(false)，代表 OCR 自动识别没通过
        val vm = viewModel()
        advanceUntilIdle()

        vm.loadTasksForAccount(passwordAccount())
        advanceUntilIdle()

        val s = vm.uiState.value
        assertTrue(s.showCaptchaDialog)
        assertFalse(s.isLoadingTasks)
    }

    @Test
    fun `密码账号自动登录成功后拉取任务`() = runTest(dispatcher) {
        repository.autoLoginResult = Result.success(true)
        repository.tasks = Triple(listOf(task(1, "早读")), emptyList(), listOf(task(3, "缺勤课")))
        val vm = viewModel()
        advanceUntilIdle()

        vm.loadTasksForAccount(passwordAccount())
        advanceUntilIdle()

        val s = vm.uiState.value
        assertFalse(s.showCaptchaDialog)
        assertEquals(listOf("早读"), s.pendingTasks.map { it.rwmc })
        assertEquals(listOf("缺勤课"), s.absentTasks.map { it.rwmc })
    }

    @Test
    fun `clearTasks 清空任务与选中账号并复位分页`() = runTest(dispatcher) {
        qrRepository.tasks = Triple(listOf(task(1, "早读")), listOf(task(2, "晚自习")), emptyList())
        val vm = viewModel()
        advanceUntilIdle()
        vm.loadTasksForAccount(qrAccount(valid = true))
        advanceUntilIdle()

        vm.clearTasks()

        val s = vm.uiState.value
        assertNull(s.selectedAccount)
        assertTrue(s.pendingTasks.isEmpty())
        assertTrue(s.completedTasks.isEmpty())
        assertEquals(6, s.displayedCompletedCount)
    }
}
