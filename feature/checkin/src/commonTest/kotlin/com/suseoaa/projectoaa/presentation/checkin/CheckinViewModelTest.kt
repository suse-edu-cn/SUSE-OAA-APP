package com.suseoaa.projectoaa.presentation.checkin

import com.suseoaa.projectoaa.domain.checkin.PasswordAutoLogin
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinAccountData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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
 * CheckinViewModel 的账号管理行为。
 *
 * 这个 ViewModel 是全应用最复杂的一个（1500+ 行，覆盖账号、登录、验证码、短信、
 * 扫码、任务六条流程），此前完全没有测试，也因此一直不敢拆。这里先把最外围、
 * 也最容易回归的账号管理钉住，为后续拆分提供保护。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CheckinViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeCheckinRepository
    private lateinit var qrRepository: FakeQrCodeCheckinRepository

    @BeforeTest
    fun setUp() {
        // viewModelScope 跑在 Dispatchers.Main 上，测试里换成可控调度器
        Dispatchers.setMain(dispatcher)
        repository = FakeCheckinRepository()
        qrRepository = FakeQrCodeCheckinRepository()
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() =
        CheckinViewModel(repository, qrRepository, PasswordAutoLogin(repository))

    private fun account(
        id: Long,
        studentId: String = "202100$id",
        name: String = "同学$id",
        loginType: Int = 0,
        location: String = "宜宾",
    ) = CheckinAccountData(
        id = id, studentId = studentId, password = "pwd", name = name,
        loginType = loginType, selectedLocation = location,
    )

    // ---------- 加载 ----------

    @Test
    fun `构造时自动加载账号列表`() = runTest(dispatcher) {
        repository.accounts += account(1)
        val vm = viewModel()

        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.accounts.size)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `加载抛异常时给出错误提示且不卡在加载中`() = runTest(dispatcher) {
        repository.loadError = IllegalStateException("数据库损坏")
        val vm = viewModel()

        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertEquals("加载账号失败: 数据库损坏", vm.uiState.value.errorMessage)
    }

    // ---------- 筛选 ----------

    @Test
    fun `按登录方式筛选账号`() = runTest(dispatcher) {
        repository.accounts += listOf(account(1, loginType = 0), account(2, loginType = 1))
        val vm = viewModel()
        advanceUntilIdle()

        vm.setAccountFilter(AccountFilterType.PASSWORD)
        assertEquals(listOf(1L), vm.getFilteredAccounts().map { it.id })

        vm.setAccountFilter(AccountFilterType.QRCODE)
        assertEquals(listOf(2L), vm.getFilteredAccounts().map { it.id })
    }

    @Test
    fun `按校区筛选账号`() = runTest(dispatcher) {
        repository.accounts += listOf(
            account(1, location = "宜宾"),
            account(2, location = "汇东"),
            account(3, location = "李白河"),
        )
        val vm = viewModel()
        advanceUntilIdle()

        vm.setAccountFilter(AccountFilterType.CAMPUS_HUIDONG)
        assertEquals(listOf(2L), vm.getFilteredAccounts().map { it.id })
    }

    @Test
    fun `默认筛选为全部`() = runTest(dispatcher) {
        repository.accounts += listOf(account(1, loginType = 0), account(2, loginType = 1))
        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(AccountFilterType.ALL, vm.uiState.value.accountFilter)
        assertEquals(2, vm.getFilteredAccounts().size)
    }

    // ---------- 新增 ----------

    @Test
    fun `学号或密码为空时拒绝添加且不落库`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.addAccount(studentId = "", password = "pwd")
        advanceUntilIdle()
        assertEquals("学号和密码不能为空", vm.uiState.value.errorMessage)

        vm.addAccount(studentId = "2021001", password = "   ")
        advanceUntilIdle()
        assertEquals("学号和密码不能为空", vm.uiState.value.errorMessage)

        assertEquals(0, repository.addAccountCalls)
    }

    @Test
    fun `学号已存在时拒绝添加`() = runTest(dispatcher) {
        repository.accounts += account(1, studentId = "2021001")
        val vm = viewModel()
        advanceUntilIdle()

        vm.addAccount(studentId = "2021001", password = "pwd")
        advanceUntilIdle()

        assertEquals("该学号已存在", vm.uiState.value.errorMessage)
        assertEquals(0, repository.addAccountCalls)
    }

    @Test
    fun `添加成功后关闭对话框并刷新列表`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.showAddDialog()
        assertTrue(vm.uiState.value.showAddDialog)

        vm.addAccount(studentId = "2021001", password = "pwd", name = "小明")
        advanceUntilIdle()

        assertEquals("添加成功", vm.uiState.value.successMessage)
        assertFalse(vm.uiState.value.showAddDialog)
        // 列表来自仓库重新拉取，能看到刚写入的账号才说明确实刷新了
        assertEquals(listOf("2021001"), vm.uiState.value.accounts.map { it.studentId })
    }

    @Test
    fun `添加失败时给出错误且不关闭对话框`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.showAddDialog()
        repository.writeError = RuntimeException("磁盘已满")

        vm.addAccount(studentId = "2021001", password = "pwd")
        advanceUntilIdle()

        assertEquals("添加失败: 磁盘已满", vm.uiState.value.errorMessage)
        assertTrue(vm.uiState.value.showAddDialog)
    }

    // ---------- 删除 ----------

    @Test
    fun `删除成功后刷新列表`() = runTest(dispatcher) {
        repository.accounts += listOf(account(1), account(2))
        val vm = viewModel()
        advanceUntilIdle()

        vm.deleteAccount(1)
        advanceUntilIdle()

        assertEquals("删除成功", vm.uiState.value.successMessage)
        assertEquals(listOf(2L), vm.uiState.value.accounts.map { it.id })
    }

    @Test
    fun `删除失败时给出错误且列表不变`() = runTest(dispatcher) {
        repository.accounts += account(1)
        val vm = viewModel()
        advanceUntilIdle()
        repository.writeError = RuntimeException("账号被占用")

        vm.deleteAccount(1)
        advanceUntilIdle()

        assertEquals("删除失败: 账号被占用", vm.uiState.value.errorMessage)
        assertEquals(listOf(1L), vm.uiState.value.accounts.map { it.id })
    }

    // ---------- 提示消息 ----------

    @Test
    fun `clearMessages 同时清掉成功与失败提示`() = runTest(dispatcher) {
        repository.accounts += account(1)
        val vm = viewModel()
        advanceUntilIdle()

        vm.deleteAccount(1)
        advanceUntilIdle()
        assertEquals("删除成功", vm.uiState.value.successMessage)

        vm.clearMessages()
        assertNull(vm.uiState.value.successMessage)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `编辑对话框开合会带上目标账号`() = runTest(dispatcher) {
        val target = account(7)
        repository.accounts += target
        val vm = viewModel()
        advanceUntilIdle()

        vm.showEditDialog(target)
        assertTrue(vm.uiState.value.showEditDialog)
        assertEquals(target, vm.uiState.value.editingAccount)

        vm.hideEditDialog()
        assertFalse(vm.uiState.value.showEditDialog)
    }
}
