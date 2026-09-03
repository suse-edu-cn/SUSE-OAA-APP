package com.suseoaa.projectoaa.shared.data.repository.checkin

import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinDetailResponse
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinLocation
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinResult
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinSubmitResponse
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinTask
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinTaskListResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * 签到任务的读取与提交。
 *
 * 通过 [CheckinTaskGateway] 屏蔽登录方式差异，密码登录与扫码登录共用这一份实现。
 * 重构前这套逻辑在 CheckinRepository 与 QrCodeCheckinRepository 中共有四份近似拷贝
 * （"WithCookies" 与 "Internal" 两两成对），任何一次接口调整都要改四处。
 *
 * 本类只负责网络与解析，不触碰数据库；账号状态的落库由调用方通过 `onStatus` 回调完成。
 */
class CheckinTaskRepository(private val json: Json) {

    /** 待签到任务（status=1） */
    suspend fun pendingTasks(gateway: CheckinTaskGateway): Result<List<CheckinTask>> =
        tasksByStatus(gateway, STATUS_PENDING)

    /** 缺勤任务（status=3） */
    suspend fun absentTasks(gateway: CheckinTaskGateway): Result<List<CheckinTask>> =
        tasksByStatus(gateway, STATUS_ABSENT)

    /**
     * 已完成任务（status=2）。
     * 列表接口不返回签到时间，需要逐个拉详情，因此只为最前面的 [initialLoadCount] 条补齐，
     * 其余的等列表滚动到时再通过 [loadCheckinTime] 懒加载。
     */
    suspend fun completedTasks(
        gateway: CheckinTaskGateway,
        initialLoadCount: Int = DEFAULT_INITIAL_LOAD_COUNT
    ): Result<List<CheckinTask>> = withContext(Dispatchers.IO) {
        tasksByStatus(gateway, STATUS_COMPLETED).map { tasks ->
            tasks.mapIndexed { index, task ->
                if (index < initialLoadCount) fillSignTime(gateway, task) else task
            }
        }
    }

    /** 三类任务一次性取回：Triple<待签到, 已完成, 缺勤> */
    suspend fun allTasks(
        gateway: CheckinTaskGateway,
        initialLoadCount: Int = DEFAULT_INITIAL_LOAD_COUNT
    ): Triple<List<CheckinTask>, List<CheckinTask>, List<CheckinTask>> = Triple(
        pendingTasks(gateway).getOrElse { emptyList() },
        completedTasks(gateway, initialLoadCount).getOrElse { emptyList() },
        absentTasks(gateway).getOrElse { emptyList() }
    )

    /** 为 [startIndex, endIndex) 区间内尚无签到时间的任务补齐签到时间 */
    suspend fun loadCheckinTime(
        gateway: CheckinTaskGateway,
        tasks: List<CheckinTask>,
        startIndex: Int,
        endIndex: Int
    ): Result<List<CheckinTask>> = withContext(Dispatchers.IO) {
        runCatching {
            println("[Checkin] 加载打卡时间: [$startIndex, $endIndex)")
            val updated = tasks.toMutableList()
            for (i in startIndex until minOf(endIndex, tasks.size)) {
                val task = tasks[i]
                if (!task.qdsj.isNullOrBlank()) continue
                updated[i] = fillSignTime(gateway, task)
            }
            updated
        }.onFailure { println("[Checkin] 批量加载打卡时间异常: ${it.message}") }
    }

    /**
     * 对指定任务签到。已签到的任务再次提交会返回 “再次签到成功”。
     * @param onStatus 用于把结果文案回写到账号记录，调用方可不传
     */
    suspend fun checkinTask(
        gateway: CheckinTaskGateway,
        taskId: Long,
        location: CheckinLocation,
        onStatus: (String) -> Unit = {}
    ): CheckinResult = withContext(Dispatchers.IO) {
        try {
            val detail = fetchTaskDetail(gateway, taskId).getOrElse { error ->
                return@withContext CheckinResult.Failed(error.message ?: "获取任务详情失败")
            }
            println("[Checkin] 任务ID: $taskId, 当前状态: ${detail.qdzt}")
            submit(gateway, taskId, location, alreadyChecked = detail.qdzt == 1, onStatus = onStatus)
        } catch (e: Exception) {
            println("[Checkin] checkinTask 异常: ${e.message}")
            onStatus("异常: ${e.message}")
            CheckinResult.Failed(e.message ?: "未知错误")
        }
    }

    /**
     * 找到今日待签到任务并签到，是自动打卡的主流程。
     * 优先匹配 “今天 + 进行中” 的任务，匹配不到则退化为列表第一条。
     */
    suspend fun checkinTodayTask(
        gateway: CheckinTaskGateway,
        location: CheckinLocation,
        onStatus: (String) -> Unit = {}
    ): CheckinResult = withContext(Dispatchers.IO) {
        try {
            val tasks = tasksByStatus(gateway, STATUS_PENDING).getOrElse { error ->
                onStatus("获取任务失败")
                return@withContext CheckinResult.Failed(error.message ?: "获取任务列表失败")
            }
            if (tasks.isEmpty()) {
                onStatus("无任务")
                return@withContext CheckinResult.NoTask("当前没有待签到的任务")
            }

            val today = CheckinClock.now().date.toString()
            val task = tasks.find { it.needTime == today && it.rwzt == "进行中" }
                ?: tasks.firstOrNull()
                ?: run {
                    onStatus("无任务")
                    return@withContext CheckinResult.NoTask("未找到今日待签到任务")
                }
            println("[Checkin] 找到任务: ${task.rwmc} (ID: ${task.id})")

            val detail = fetchTaskDetail(gateway, task.id).getOrElse { error ->
                return@withContext CheckinResult.Failed(error.message ?: "获取任务详情失败")
            }
            if (detail.qdzt == 1) {
                onStatus("已签到")
                return@withContext CheckinResult.AlreadyChecked("今日已签到，无需重复操作")
            }

            submit(gateway, task.id, location, alreadyChecked = false, onStatus = onStatus)
        } catch (e: Exception) {
            println("[Checkin] checkinTodayTask 异常: ${e.message}")
            onStatus("异常: ${e.message}")
            CheckinResult.Failed(e.message ?: "未知错误")
        }
    }

    // ==================== 内部实现 ====================

    /** 拉取某一状态的任务并按时间倒序（最近的在最前） */
    private suspend fun tasksByStatus(
        gateway: CheckinTaskGateway,
        status: Int
    ): Result<List<CheckinTask>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = gateway.taskList(status)
            if (response.status.value != 200) {
                throw CheckinException("获取任务列表失败 (${response.status.value})")
            }
            val body = json.decodeFromString<CheckinTaskListResponse>(response.bodyAsText())
            if (!body.success || body.resultCode != 0) {
                throw CheckinException(body.errorMsg ?: "获取任务列表失败")
            }
            val tasks = body.result?.data.orEmpty()
                .sortedByDescending { task -> task.needTime.ifEmpty { task.qdksrq } }
            println("[Checkin] 获取到 ${tasks.size} 个任务 (status=$status)")
            tasks
        }.onFailure { println("[Checkin] 获取任务列表异常(status=$status): ${it.message}") }
    }

    /** 拉取任务详情中的打卡信息（dkxx） */
    private suspend fun fetchTaskDetail(
        gateway: CheckinTaskGateway,
        taskId: Long
    ): Result<CheckinTaskDetail> = runCatching {
        val response = gateway.taskDetail(taskId)
        if (response.status.value != 200) {
            throw CheckinException("获取任务详情失败 (${response.status.value})")
        }
        val body = json.decodeFromString<CheckinDetailResponse>(response.bodyAsText())
        if (!body.success || body.resultCode != 0) {
            throw CheckinException("获取任务详情失败: ${body.errorMsg}")
        }
        val dkxx = body.result?.data?.dkxx ?: throw CheckinException("任务详情数据为空")
        CheckinTaskDetail(qdzt = dkxx.qdzt, qdsj = dkxx.qdsj)
    }

    /** 用详情里的签到时间补全任务；失败时原样返回，不影响列表展示 */
    private suspend fun fillSignTime(gateway: CheckinTaskGateway, task: CheckinTask): CheckinTask {
        val detail = fetchTaskDetail(gateway, task.id).getOrNull() ?: return task
        return if (!detail.qdsj.isNullOrBlank()) {
            task.copy(qdsj = detail.qdsj, qdzt = detail.qdzt)
        } else {
            task
        }
    }

    /** 提交签到并把结果文案回写给调用方 */
    private suspend fun submit(
        gateway: CheckinTaskGateway,
        taskId: Long,
        location: CheckinLocation,
        alreadyChecked: Boolean,
        onStatus: (String) -> Unit
    ): CheckinResult {
        val signData = CheckinSignData.build(taskId, location)
        println("[Checkin] 签到数据: $signData")

        val response = gateway.submitCheckin(signData)
        val responseText = response.bodyAsText()
        println("[Checkin] 签到响应: $responseText")

        val result = json.decodeFromString<CheckinSubmitResponse>(responseText)
        return if (result.success && result.resultCode == 0) {
            onStatus("成功")
            CheckinResult.Success(if (alreadyChecked) "再次签到成功" else "签到成功")
        } else {
            val errorMsg = result.errorMsg ?: "签到失败"
            onStatus("失败: $errorMsg")
            CheckinResult.Failed(errorMsg)
        }
    }

    private data class CheckinTaskDetail(val qdzt: Int, val qdsj: String?)

    private companion object {
        const val STATUS_PENDING = 1
        const val STATUS_COMPLETED = 2
        const val STATUS_ABSENT = 3
        const val DEFAULT_INITIAL_LOAD_COUNT = 5
    }
}

/** 签到链路中的业务异常，用于把失败原因带到 Result 外层 */
class CheckinException(message: String) : Exception(message)
