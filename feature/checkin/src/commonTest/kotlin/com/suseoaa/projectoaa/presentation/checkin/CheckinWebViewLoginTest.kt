package com.suseoaa.projectoaa.presentation.checkin

import com.suseoaa.projectoaa.domain.checkin.PasswordAutoLogin
import com.suseoaa.projectoaa.shared.domain.model.checkin.SopSessionUser
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinAccountData
import com.suseoaa.projectoaa.shared.domain.model.checkin.EduUserInfo
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
 * 微信扫码（WebView）添加账号的流程。
 *
 * 这条链路有多级兜底：先从 _sop_session_ JWT 里取学号，取不到再调接口；
 * 拿到学号后还要再换一次签到专用的 SESSION 才能落库。任一环失败都要给出
 * 可分辨的提示，否则用户只会看到"添加失败"而不知道卡在哪一步。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CheckinWebViewLoginTest {

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

    private val cookies = mapOf("_sop_session_" to "jwt-token", "SESSION" to "abc")

    @Test
    fun `打开与关闭 WebView 登录对话框`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.showWebViewLoginDialog()
        assertTrue(vm.uiState.value.showWebViewLoginDialog)

        vm.hideWebViewLoginDialog()
        assertFalse(vm.uiState.value.showWebViewLoginDialog)
    }

    @Test
    fun `优先从 JWT 取用户信息并落库`() = runTest(dispatcher) {
        qrRepository.sopSessionUser = SopSessionUser(studentId = "2021001", name = "小明")
        val vm = viewModel()
        advanceUntilIdle()

        vm.onWebViewLoginSuccess(cookies)
        advanceUntilIdle()

        assertEquals("2021001", qrRepository.savedAccount?.first)
        assertEquals("小明", qrRepository.savedAccount?.second)
        // 落库用的必须是换过的签到专用 SESSION，而不是 WebView 原始 cookie
        assertEquals("full-cookies", qrRepository.savedAccount?.third)
        assertFalse(vm.uiState.value.showWebViewLoginDialog)
        assertTrue(vm.uiState.value.successMessage?.contains("2021001") == true)
    }

    @Test
    fun `JWT 取不到时回落到接口`() = runTest(dispatcher) {
        qrRepository.sopSessionUser = null
        qrRepository.eduUserInfo = Result.success(EduUserInfo(code = "2021002", name = "小红"))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onWebViewLoginSuccess(cookies)
        advanceUntilIdle()

        assertEquals("2021002", qrRepository.savedAccount?.first)
        assertEquals("小红", qrRepository.savedAccount?.second)
    }

    @Test
    fun `两级兜底都拿不到学号时给出明确提示`() = runTest(dispatcher) {
        qrRepository.sopSessionUser = null
        qrRepository.eduUserInfo = Result.success(EduUserInfo(code = null))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onWebViewLoginSuccess(cookies)
        advanceUntilIdle()

        assertNull(qrRepository.savedAccount)
        assertTrue(vm.uiState.value.errorMessage?.contains("获取学号失败") == true)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `学号已存在时不重复添加`() = runTest(dispatcher) {
        repository.accounts += CheckinAccountData(
            id = 1, studentId = "2021001", password = "", loginType = 1)
        qrRepository.sopSessionUser = SopSessionUser(studentId = "2021001", name = "小明")
        val vm = viewModel()
        advanceUntilIdle()

        vm.onWebViewLoginSuccess(cookies)
        advanceUntilIdle()

        assertNull(qrRepository.savedAccount)
        assertEquals("该学号账号已存在", vm.uiState.value.errorMessage)
    }

    @Test
    fun `换取签到 SESSION 失败时不落库并说明卡在授权`() = runTest(dispatcher) {
        qrRepository.sopSessionUser = SopSessionUser(studentId = "2021001", name = "小明")
        qrRepository.ssoResult = Result.failure(IllegalStateException("SSO 拒绝"))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onWebViewLoginSuccess(cookies)
        advanceUntilIdle()

        assertNull(qrRepository.savedAccount)
        assertTrue(vm.uiState.value.errorMessage?.contains("获取签到授权失败") == true)
    }

    @Test
    fun `落库失败时保留对话框并提示`() = runTest(dispatcher) {
        qrRepository.sopSessionUser = SopSessionUser(studentId = "2021001", name = "小明")
        qrRepository.saveResult = Result.failure(RuntimeException("磁盘已满"))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onWebViewLoginSuccess(cookies)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.errorMessage?.contains("磁盘已满") == true)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `WebView 报错时提示用户`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onWebViewLoginError("网络中断")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.errorMessage?.contains("网络中断") == true)
    }
}
