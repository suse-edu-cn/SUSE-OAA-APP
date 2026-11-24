package com.suseoaa.projectoaa.presentation

import com.suseoaa.projectoaa.presentation.appmanagement.AppManagementViewModel
import com.suseoaa.projectoaa.shared.domain.model.oauth.OaaApp
import com.suseoaa.projectoaa.shared.domain.repository.AppManagementRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class AppManagementViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun loadsCreatesUpdatesAndDeletesAppsWithValidation() = runTest(dispatcher) {
        val repo = FakeAppRepo(
            mutableListOf(
                OaaApp(
                    clientId = "suse_oj",
                    maskSecret = "oaa_sec_****bc0d",
                    name = "oj 平台",
                    redirectUris = "https://oj.example/cb",
                    scope = "openid profile",
                    isActive = true
                )
            )
        )
        val vm = AppManagementViewModel(repo)
        advanceUntilIdle()
        assertEquals(listOf("suse_oj"), vm.uiState.value.apps.map { it.clientId })

        // Validation blocks empty fields
        vm.createApp("", "名称", "https://cb", "openid")
        assertEquals("Client ID 不能为空", vm.uiState.value.message)
        assertTrue(repo.createCalls == 0)

        // Create exposes one-time secret and refreshes list
        vm.createApp(" new_app ", " 新应用 ", " https://app.example/cb ", "openid  email openid")
        advanceUntilIdle()
        assertEquals("oaa_sec_one_time", vm.uiState.value.createdSecretApp?.clientSecret)
        assertEquals("openid email", repo.apps.last().scope)
        assertEquals(2, vm.uiState.value.apps.size)

        vm.dismissCreatedSecret()
        assertNull(vm.uiState.value.createdSecretApp)

        // Update normalizes scope and supports disabling
        vm.updateApp("new_app", "新应用2", "https://app.example/cb2", "level  profile", isActive = false)
        advanceUntilIdle()
        val updated = vm.uiState.value.apps.first { it.clientId == "new_app" }
        assertFalse(updated.isActive)
        assertEquals("level profile", updated.scope)

        // Delete removes app from list
        vm.deleteApp("new_app")
        advanceUntilIdle()
        assertEquals(listOf("suse_oj"), vm.uiState.value.apps.map { it.clientId })
    }

    private class FakeAppRepo(val apps: MutableList<OaaApp>) : AppManagementRepository {
        var createCalls = 0

        override suspend fun listApps(): Result<List<OaaApp>> = Result.success(apps.toList())

        override suspend fun getAppDetail(clientId: String): Result<OaaApp> =
            Result.success(apps.first { it.clientId == clientId })

        override suspend fun createApp(
            clientId: String,
            name: String,
            redirectUris: String,
            scope: String
        ): Result<OaaApp> {
            createCalls++
            val created = OaaApp(
                clientId = clientId,
                clientSecret = "oaa_sec_one_time",
                maskSecret = "oaa_sec_****time",
                name = name,
                redirectUris = redirectUris,
                scope = scope,
                isActive = true
            )
            apps += created
            return Result.success(created)
        }

        override suspend fun updateApp(
            clientId: String,
            name: String,
            redirectUris: String,
            scope: String,
            isActive: Boolean
        ): Result<Unit> {
            val idx = apps.indexOfFirst { it.clientId == clientId }
            apps[idx] = apps[idx].copy(
                name = name,
                redirectUris = redirectUris,
                scope = scope,
                isActive = isActive
            )
            return Result.success(Unit)
        }

        override suspend fun deleteApp(clientId: String): Result<Unit> {
            apps.removeAll { it.clientId == clientId }
            return Result.success(Unit)
        }
    }
}
