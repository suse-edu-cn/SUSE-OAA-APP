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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 密码登录的二次验证：图形验证码与短信验证码。
 *
 * 这两条路互为回退——验证码登录被判定需要短信时要切到短信对话框，短信取消后
 * 又要能回到验证码。加上"登录完成后按入口决定是继续打卡还是加载任务"，
 * 是 CheckinViewModel 里状态最绕的一段。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CheckinPasswordChallengeTest {

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

    private val account = CheckinAccountData(
        id = 1, studentId = "2021001", password = "pwd", name = "小明", loginType = 0)

    private fun viewModel() =
        CheckinViewModel(repository, qrRepository, PasswordAutoLogin(repository))

    /**
     * 走到验证码对话框：密码账号加载任务时自动登录失败，就会停在这里，
     * 且入口被记为「任务列表」。
     */
    private fun CheckinViewModel.openCaptchaViaTasks() {
        repository.accounts += account
        loadTasksForAccount(account)
    }

    @Test
    fun `自动登录失败会停在验证码对话框并拉取验证码图片`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.openCaptchaViaTasks()
        advanceUntilIdle()

        val s = vm.uiState.value
        assertTrue(s.showCaptchaDialog)
        assertEquals(account.id, s.currentCheckingAccount?.id)
        assertNotNull(s.captchaImageBytes)
        assertFalse(s.isLoadingCaptcha)
    }

    @Test
    fun `验证码为空时提示且不发起登录`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.openCaptchaViaTasks()
        advanceUntilIdle()

        vm.submitCaptchaAndCheckin("   ")
        advanceUntilIdle()

        assertEquals("请输入验证码", vm.uiState.value.errorMessage)
        assertEquals(0, repository.loginWithCaptchaCalls)
    }

    @Test
    fun `验证码登录被判定需要短信时切到短信对话框`() = runTest(dispatcher) {
        repository.loginResult = Result.failure(IllegalStateException("需要短信验证"))
        repository.smsRequired = true
        val vm = viewModel()
        advanceUntilIdle()
        vm.openCaptchaViaTasks()
        advanceUntilIdle()

        vm.submitCaptchaAndCheckin("8888")
        advanceUntilIdle()

        val s = vm.uiState.value
        assertTrue(s.showSmsDialog)
        assertFalse(s.showCaptchaDialog)
        assertEquals("138****0000", s.smsMaskedPhone)
    }

    @Test
    fun `验证码登录普通失败时报错并重新拉取验证码`() = runTest(dispatcher) {
        repository.loginResult = Result.failure(IllegalStateException("验证码错误"))
        repository.smsRequired = false
        val vm = viewModel()
        advanceUntilIdle()
        vm.openCaptchaViaTasks()
        advanceUntilIdle()

        vm.submitCaptchaAndCheckin("0000")
        advanceUntilIdle()

        val s = vm.uiState.value
        assertEquals("验证码错误", s.errorMessage)
        assertTrue(s.showCaptchaDialog, "失败后应停留在验证码对话框")
        assertFalse(s.isLoggingIn)
        assertNotNull(s.captchaImageBytes)   // 失败后自动重新拉取了新验证码
        assertEquals(1, repository.loginWithCaptchaCalls)
    }

    @Test
    fun `入口为任务列表时登录成功直接去加载任务`() = runTest(dispatcher) {
        repository.loginResult = Result.success(Unit)
        repository.tasks = Triple(listOf(CheckinTask(id = 1, rwmc = "早读")), emptyList(), emptyList())
        val vm = viewModel()
        advanceUntilIdle()
        vm.openCaptchaViaTasks()
        advanceUntilIdle()

        vm.submitCaptchaAndCheckin("8888")
        advanceUntilIdle()

        val s = vm.uiState.value
        assertFalse(s.showCaptchaDialog)
        assertEquals(listOf("早读"), s.pendingTasks.map { it.rwmc })
    }

    @Test
    fun `刷新验证码失败时给出提示`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.openCaptchaViaTasks()
        advanceUntilIdle()

        repository.captchaImage = Result.failure(IllegalStateException("服务不可用"))
        vm.refreshCaptcha()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.errorMessage?.contains("获取验证码失败") == true)
        assertFalse(vm.uiState.value.isLoadingCaptcha)
    }

    @Test
    fun `发送短信后立即进入倒计时，倒计时期间不重复发送`() = runTest(dispatcher) {
        repository.loginResult = Result.failure(IllegalStateException("需要短信验证"))
        repository.smsRequired = true
        val vm = viewModel()
        advanceUntilIdle()
        vm.openCaptchaViaTasks()
        advanceUntilIdle()
        vm.submitCaptchaAndCheckin("8888")
        advanceUntilIdle()

        val before = repository.sendSmsCalls
        vm.sendSmsCode()
        // 倒计时秒数是同步置上的，此处刻意不推进虚拟时间——
        // advanceUntilIdle() 会把 30 秒定时器一次性走完，倒计时就归零了
        assertEquals(30, vm.uiState.value.smsResendCountdownSeconds)

        vm.sendSmsCode()   // 倒计时未结束，应被忽略
        advanceUntilIdle()
        assertEquals(before + 1, repository.sendSmsCalls, "倒计时期间的重复点击不应再次发送")
    }

    @Test
    fun `倒计时走完后可以再次发送`() = runTest(dispatcher) {
        repository.loginResult = Result.failure(IllegalStateException("需要短信验证"))
        repository.smsRequired = true
        val vm = viewModel()
        advanceUntilIdle()
        vm.openCaptchaViaTasks()
        advanceUntilIdle()
        vm.submitCaptchaAndCheckin("8888")
        advanceUntilIdle()

        val before = repository.sendSmsCalls
        vm.sendSmsCode()
        advanceUntilIdle()   // 走完 30 秒
        assertEquals(0, vm.uiState.value.smsResendCountdownSeconds)

        vm.sendSmsCode()
        advanceUntilIdle()
        assertEquals(before + 2, repository.sendSmsCalls)
    }

    @Test
    fun `短信验证码为空时提示且不提交`() = runTest(dispatcher) {
        repository.loginResult = Result.failure(IllegalStateException("需要短信验证"))
        repository.smsRequired = true
        val vm = viewModel()
        advanceUntilIdle()
        vm.openCaptchaViaTasks()
        advanceUntilIdle()
        vm.submitCaptchaAndCheckin("8888")
        advanceUntilIdle()

        vm.submitSmsCodeAndCheckin("")
        advanceUntilIdle()

        assertEquals("请输入短信验证码", vm.uiState.value.errorMessage)
        assertTrue(vm.uiState.value.showSmsDialog)
    }

    @Test
    fun `取消短信验证会清掉挂起的挑战并关闭对话框`() = runTest(dispatcher) {
        repository.loginResult = Result.failure(IllegalStateException("需要短信验证"))
        repository.smsRequired = true
        val vm = viewModel()
        advanceUntilIdle()
        vm.openCaptchaViaTasks()
        advanceUntilIdle()
        vm.submitCaptchaAndCheckin("8888")
        advanceUntilIdle()

        val before = repository.clearPendingSmsCalls
        vm.cancelSmsVerification()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.showSmsDialog)
        assertEquals(before + 1, repository.clearPendingSmsCalls)
        assertEquals(0, vm.uiState.value.smsResendCountdownSeconds)
    }
}
