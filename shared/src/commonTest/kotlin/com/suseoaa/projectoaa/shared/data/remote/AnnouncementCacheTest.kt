package com.suseoaa.projectoaa.shared.data.remote

import com.suseoaa.projectoaa.shared.data.local.store.CredentialStore
import com.suseoaa.projectoaa.shared.data.local.store.UserDataCleaner
import com.suseoaa.projectoaa.shared.data.local.store.UserProfileStore
import com.suseoaa.projectoaa.shared.data.remote.api.OaaAnnouncementApi
import com.suseoaa.projectoaa.shared.data.remote.api.OaaAuthApi
import com.suseoaa.projectoaa.shared.data.remote.api.OaaOrganizationApi
import com.suseoaa.projectoaa.shared.data.remote.api.OaaUserApi
import com.suseoaa.projectoaa.shared.data.repository.AnnouncementRepositoryImpl
import com.suseoaa.projectoaa.shared.data.repository.OrganizationRepositoryImpl
import com.suseoaa.projectoaa.shared.data.repository.PersonRepositoryImpl
import com.suseoaa.projectoaa.shared.domain.model.announcement.AnnouncementStatus
import io.ktor.http.encodedPath
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AnnouncementCacheTest {

    @Test
    fun freshCacheHitSkipsDuplicateNetworkRequest() = runTest {
        val stack = OaaTestStack { request ->
            when (request.url.encodedPath) {
                "/v2/announcement/list" -> json(
                    """{"code":200,"message":"success","data":[{"announcement_id":1,"title":"招新通知","content":"正文","department_name":"协会","is_active":true}]}"""
                )
                else -> error("unexpected ${request.url}")
            }
        }
        val repo = AnnouncementRepositoryImpl(OaaAnnouncementApi(stack.requester), testJson, stack.sessionStore)

        val first = repo.getAnnouncements(AnnouncementStatus.Active).getOrThrow()
        assertEquals(1, stack.requestsTo("/announcement/list").size)
        assertEquals(1, first.size)
        assertTrue(repo.isCacheFresh(AnnouncementStatus.Active))
        assertEquals("招新通知", repo.getCachedAnnouncements(AnnouncementStatus.Active)?.single()?.title)
        assertEquals("招新通知", repo.getCachedAnnouncement(1)?.title)

        val second = repo.getAnnouncements(AnnouncementStatus.Active).getOrThrow()
        assertEquals(1, stack.requestsTo("/announcement/list").size, "TTL 内应直接命中内存缓存")
        assertEquals(first, second)
    }

    @Test
    fun concurrentRequestsCoalesceIntoSingleNetworkCall() = runTest {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val stack = OaaTestStack { request ->
            when (request.url.encodedPath) {
                "/v2/announcement/list" -> {
                    entered.complete(Unit)
                    release.await()
                    json(
                        """{"code":200,"message":"success","data":[{"announcement_id":2,"title":"历史公告","content":"内容","department_name":"算法竞赛部","is_active":false}]}"""
                    )
                }
                else -> error("unexpected ${request.url}")
            }
        }
        val repo = AnnouncementRepositoryImpl(OaaAnnouncementApi(stack.requester), testJson, stack.sessionStore)

        val d1 = async { repo.getAnnouncements(AnnouncementStatus.History) }
        entered.await()
        val d2 = async { repo.getAnnouncements(AnnouncementStatus.History) }
        release.complete(Unit)

        val results = listOf(d1, d2).awaitAll()
        assertEquals(1, stack.requestsTo("/announcement/list").size, "并发请求同一状态应合并为一次网络请求")
        assertEquals("历史公告", results[0].getOrThrow().single().title)
        assertEquals("历史公告", results[1].getOrThrow().single().title)
    }

    @Test
    fun prefetchWarmsCacheAndSubsequentLoadHitsCache() = runTest {
        val stack = OaaTestStack { request ->
            val status = request.url.parameters["status"]
            json(
                """{"code":200,"message":"success","data":[{"announcement_id":${if (status == "history") 10 else 20},"title":"$status","content":"body","department_name":"秘书处"}]}"""
            )
        }
        val repo = AnnouncementRepositoryImpl(OaaAnnouncementApi(stack.requester), testJson, stack.sessionStore)

        repo.prefetch(listOf(AnnouncementStatus.History, AnnouncementStatus.Draft))
        assertEquals(2, stack.requestsTo("/announcement/list").size)

        val history = repo.getAnnouncements(AnnouncementStatus.History).getOrThrow()
        val draft = repo.getAnnouncements(AnnouncementStatus.Draft).getOrThrow()
        assertEquals("history", history.single().title)
        assertEquals("draft", draft.single().title)
        assertEquals(2, stack.requestsTo("/announcement/list").size, "预加载完成后读取不应再次发请求")
    }

    @Test
    fun mutationsMarkCacheDirtyWhileKeepingSynchronousFallback() = runTest {
        var version = 1
        val stack = OaaTestStack { request ->
            when (request.url.encodedPath) {
                "/v2/announcement/list" -> json(
                    """{"code":200,"message":"success","data":[{"announcement_id":5,"title":"标题v$version","content":"内容v$version","department_name":"组织宣传部","is_active":true}]}"""
                )
                "/v2/announcement/update" -> {
                    version = 2
                    json("""{"code":200,"message":"success","data":null}""")
                }
                else -> error("unexpected ${request.url}")
            }
        }
        val repo = AnnouncementRepositoryImpl(OaaAnnouncementApi(stack.requester), testJson, stack.sessionStore)

        repo.getAnnouncements(AnnouncementStatus.Active).getOrThrow()
        assertTrue(repo.isCacheFresh(AnnouncementStatus.Active))

        repo.updateAnnouncement(id = 5, title = "标题v2", content = "内容v2").getOrThrow()
        assertFalse(repo.isCacheFresh(AnnouncementStatus.Active), "编辑后应标脏触发静默刷新")
        assertEquals("标题v1", repo.getCachedAnnouncements(AnnouncementStatus.Active)?.single()?.title)

        val refreshed = repo.getAnnouncements(AnnouncementStatus.Active).getOrThrow()
        assertEquals("标题v2", refreshed.single().title)
        assertTrue(repo.isCacheFresh(AnnouncementStatus.Active))
    }

    @Test
    fun sessionTokenChangeInvalidatesCachedAnnouncementsAndCurrentUser() = runTest {
        var userIndex = 1
        val stack = OaaTestStack { request ->
            when (request.url.encodedPath) {
                "/v2/announcement/list" -> json(
                    """{"code":200,"message":"success","data":[{"announcement_id":$userIndex,"title":"公告$userIndex","content":"正文","department_name":"协会"}]}"""
                )
                "/v2/user/me" -> json(
                    """{"code":200,"message":"success","data":{"user_id":$userIndex,"student_id":"2024000$userIndex","username":"user$userIndex","name":"用户$userIndex","department":"协会","role":"会长","role_level":100}}"""
                )
                "/v2/role/list" -> json("""{"code":200,"message":"success","data":[]}""")
                "/v2/department/list" -> json("""{"code":200,"message":"success","data":[{"id":1,"name":"协会","type":"协会","is_active":true}]}""")
                else -> error("unexpected ${request.url}")
            }
        }
        val prefs = InMemoryPreferences()
        val cleaner = UserDataCleaner(
            sessionStore = stack.sessionStore,
            credentialStore = CredentialStore(prefs),
            userProfileStore = UserProfileStore(prefs),
            clearNetworkSessions = {}
        )
        val orgRepo = OrganizationRepositoryImpl(OaaOrganizationApi(stack.requester))
        val personRepo = PersonRepositoryImpl(
            userApi = OaaUserApi(stack.requester),
            authApi = OaaAuthApi(stack.requester),
            organizationRepository = orgRepo,
            userDataCleaner = cleaner,
            sessionStore = stack.sessionStore
        )
        val announcementRepo = AnnouncementRepositoryImpl(
            api = OaaAnnouncementApi(stack.requester),
            json = testJson,
            sessionStore = stack.sessionStore
        )

        stack.sessionStore.saveTokens("token-user-1", "refresh-1")
        announcementRepo.getAnnouncements(AnnouncementStatus.Active).getOrThrow()
        personRepo.getCurrentUser().getOrThrow()
        assertNotNull(announcementRepo.getCachedAnnouncements(AnnouncementStatus.Active))
        assertEquals("user1", personRepo.getCachedCurrentUser()?.person?.username)

        // 切换用户 token 后，旧用户的内存缓存必须失效，避免串号或越权草稿残留
        userIndex = 2
        stack.sessionStore.saveTokens("token-user-2", "refresh-2")

        assertNull(announcementRepo.getCachedAnnouncements(AnnouncementStatus.Active))
        assertFalse(announcementRepo.isCacheFresh(AnnouncementStatus.Active))
        assertNull(personRepo.getCachedCurrentUser())

        val user2 = personRepo.getCurrentUser().getOrThrow()
        val ann2 = announcementRepo.getAnnouncements(AnnouncementStatus.Active).getOrThrow()
        assertEquals("user2", user2.person.username)
        assertEquals("公告2", ann2.single().title)
    }
}
