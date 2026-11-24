package com.suseoaa.projectoaa.shared.data.repository

import com.suseoaa.projectoaa.shared.data.local.store.SessionStore
import com.suseoaa.projectoaa.shared.data.remote.api.OaaAnnouncementApi
import com.suseoaa.projectoaa.shared.data.remote.api.decodeListLenient
import com.suseoaa.projectoaa.shared.domain.error.AppError
import com.suseoaa.projectoaa.shared.domain.error.appFailure
import com.suseoaa.projectoaa.shared.domain.model.announcement.Announcement
import com.suseoaa.projectoaa.shared.domain.model.announcement.AnnouncementIdRequest
import com.suseoaa.projectoaa.shared.domain.model.announcement.AnnouncementStatus
import com.suseoaa.projectoaa.shared.domain.model.announcement.CreateAnnouncementRequest
import com.suseoaa.projectoaa.shared.domain.model.announcement.UpdateAnnouncementRequest
import com.suseoaa.projectoaa.shared.domain.repository.AnnouncementRepository
import com.suseoaa.projectoaa.shared.util.OaaClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlin.concurrent.Volatile

/**
 * 公告信息仓库（带会话级 SWR 内存缓存与并发请求合并）。
 */
class AnnouncementRepositoryImpl(
    private val api: OaaAnnouncementApi,
    private val json: Json,
    private val sessionStore: SessionStore? = null
) : AnnouncementRepository {

    private data class CacheEntry(
        val items: List<Announcement>,
        val fetchedAtMs: Long,
        val dirty: Boolean = false
    )

    private val mutex = Mutex()
    private val fetchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var cacheSnapshot: Map<AnnouncementStatus?, CacheEntry> = emptyMap()

    @Volatile
    private var cachedSessionToken: String? = null

    private var cacheGeneration: Long = 0L
    private val inFlight = mutableMapOf<AnnouncementStatus?, Deferred<Result<List<Announcement>>>>()

    private fun isCurrentSession(): Boolean =
        sessionStore == null || sessionStore.cachedToken == cachedSessionToken

    private fun syncSessionLocked() {
        val currentToken = sessionStore?.cachedToken
        if (sessionStore != null && currentToken != cachedSessionToken) {
            cachedSessionToken = currentToken
            cacheSnapshot = emptyMap()
            cacheGeneration++
            inFlight.values.forEach { it.cancel() }
            inFlight.clear()
        }
    }

    override fun getCachedAnnouncements(status: AnnouncementStatus?): List<Announcement>? {
        if (!isCurrentSession()) return null
        val snapshot = cacheSnapshot
        if (status != null) {
            return snapshot[status]?.items
        }
        snapshot[null]?.items?.let { return it }
        val merged = listOfNotNull(
            snapshot[AnnouncementStatus.Active]?.items,
            snapshot[AnnouncementStatus.Draft]?.items,
            snapshot[AnnouncementStatus.History]?.items
        ).flatten()
        return if (merged.isEmpty()) null else merged.distinctBy { it.id }
    }

    override fun getCachedAnnouncement(id: Int): Announcement? {
        if (!isCurrentSession()) return null
        val snapshot = cacheSnapshot
        val searchOrder = listOf(
            AnnouncementStatus.Active,
            AnnouncementStatus.Draft,
            AnnouncementStatus.History,
            null
        )
        for (status in searchOrder) {
            snapshot[status]?.items?.firstOrNull { it.id == id }?.let { return it }
        }
        return null
    }

    override fun isCacheFresh(status: AnnouncementStatus?): Boolean {
        if (!isCurrentSession()) return false
        val entry = cacheSnapshot[status] ?: return false
        if (entry.dirty) return false
        val ageMs = OaaClock.now().toEpochMilliseconds() - entry.fetchedAtMs
        return ageMs < FRESH_TTL_MS
    }

    override suspend fun getAnnouncements(
        status: AnnouncementStatus?,
        forceRefresh: Boolean
    ): Result<List<Announcement>> {
        val deferred: Deferred<Result<List<Announcement>>> = mutex.withLock {
            syncSessionLocked()
            val entry = cacheSnapshot[status]
            if (!forceRefresh && entry != null && !entry.dirty) {
                val ageMs = OaaClock.now().toEpochMilliseconds() - entry.fetchedAtMs
                if (ageMs < FRESH_TTL_MS) {
                    return Result.success(entry.items)
                }
            }

            if (!forceRefresh) {
                val existing = inFlight[status]
                if (existing != null && existing.isActive) {
                    return@withLock existing
                }
            }

            val requestGen = cacheGeneration
            val requestToken = cachedSessionToken
            lateinit var newDeferred: Deferred<Result<List<Announcement>>>
            newDeferred = fetchScope.async {
                try {
                    val result = api.list(status = status?.value, withContent = true).map { it.orEmpty() }
                    result.onSuccess { items ->
                        mutex.withLock {
                            val tokenMatches = sessionStore == null || sessionStore.cachedToken == requestToken
                            if (tokenMatches && cacheGeneration == requestGen) {
                                val nextMap = cacheSnapshot.toMutableMap()
                                nextMap[status] = CacheEntry(
                                    items = items,
                                    fetchedAtMs = OaaClock.now().toEpochMilliseconds(),
                                    dirty = false
                                )
                                cacheSnapshot = nextMap
                            }
                        }
                    }
                    result
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Result.failure(e)
                } finally {
                    mutex.withLock {
                        if (inFlight[status] === newDeferred) {
                            inFlight.remove(status)
                        }
                    }
                }
            }
            inFlight[status] = newDeferred
            newDeferred
        }

        return deferred.await()
    }

    override suspend fun prefetch(statuses: Collection<AnnouncementStatus>) {
        val targets = statuses.distinct().filter { !isCacheFresh(it) }
        if (targets.isEmpty()) return
        coroutineScope {
            targets.forEach { status ->
                launch {
                    getAnnouncements(status = status, forceRefresh = false)
                }
            }
        }
    }

    override fun invalidateCache() {
        cacheSnapshot = emptyMap()
        cacheGeneration++
        cachedSessionToken = sessionStore?.cachedToken
        inFlight.values.forEach { it.cancel() }
        inFlight.clear()
    }

    private suspend fun markDirty() {
        mutex.withLock {
            cacheGeneration++
            inFlight.values.forEach { it.cancel() }
            inFlight.clear()
            if (cacheSnapshot.isNotEmpty()) {
                cacheSnapshot = cacheSnapshot.mapValues { (_, entry) -> entry.copy(dirty = true) }
            }
        }
    }

    override suspend fun getAnnouncement(id: Int): Result<Announcement> {
        val element = api.get(id).getOrElse { return Result.failure(it) }
        val announcement = runCatching { json.decodeListLenient<Announcement>(element).firstOrNull() }.getOrNull()
            ?: return appFailure(AppError.Business(userMessage = "公告不存在或已被删除"))
        return Result.success(announcement)
    }

    override suspend fun createDraft(departmentId: Int, title: String, content: String): Result<Unit> =
        api.create(CreateAnnouncementRequest(departmentId = departmentId, title = title, content = content))
            .onSuccess { markDirty() }

    override suspend fun createAndPublish(
        departmentId: Int,
        departmentName: String,
        title: String,
        content: String
    ): Result<Unit> {
        createDraft(departmentId, title, content).getOrElse { return Result.failure(it) }

        val created = api.list(status = AnnouncementStatus.Draft.value, withContent = false)
            .getOrNull()
            .orEmpty()
            .filter { it.departmentName == departmentName && it.title == title }
            .maxByOrNull { it.id }
            ?: return appFailure(
                AppError.Business(userMessage = "公告已保存为草稿，但未能自动发布，请在草稿中手动发布")
            )
        return publishAnnouncement(created.id)
    }

    override suspend fun updateAnnouncement(id: Int, title: String, content: String): Result<Unit> =
        api.update(UpdateAnnouncementRequest(announcementId = id, title = title, content = content))
            .onSuccess { markDirty() }

    override suspend fun publishAnnouncement(id: Int): Result<Unit> =
        api.push(AnnouncementIdRequest(id))
            .onSuccess { markDirty() }

    override suspend fun deleteAnnouncement(id: Int): Result<Unit> =
        api.delete(AnnouncementIdRequest(id))
            .onSuccess { markDirty() }

    private companion object {
        const val FRESH_TTL_MS = 30_000L
    }
}
