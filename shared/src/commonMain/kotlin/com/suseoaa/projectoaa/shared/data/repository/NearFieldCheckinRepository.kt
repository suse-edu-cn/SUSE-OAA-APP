package com.suseoaa.projectoaa.shared.data.repository

import com.suseoaa.projectoaa.shared.database.CourseDatabase
import com.suseoaa.projectoaa.shared.database.NearFieldTask
import com.suseoaa.projectoaa.shared.database.NearFieldParticipant
import com.suseoaa.projectoaa.shared.domain.nearfield.NearFieldCheckinTask
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 近场签到业务仓库
 * 负责处理数据的本地持久化以及点对点通信逻辑
 */
class NearFieldCheckinRepository(
    private val database: CourseDatabase,
    private val json: Json
) {
    private val queries = database.nearFieldCheckinQueries
    private val repositoryScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var serverJob: Job? = null

    /**
     * 保存或更新签到任务到本地数据库
     */
    suspend fun saveTask(task: NearFieldCheckinTask, isMyHosted: Boolean) {
        withContext(Dispatchers.Default) {
            queries.insertTask(
                taskIdentifier = task.taskIdentifier,
                activityName = task.activityName,
                hostName = task.hostDeviceName,
                startTime = task.startTime,
                endTime = task.endTime,
                publishTimestamp = task.publishTimestamp,
                securityNonce = task.securityNonce,
                isMyHosted = if (isMyHosted) 1L else 0L
            )
        }
    }

    /**
     * 获取所有本地保存的签到任务历史
     */
    fun getTaskHistory(): Flow<List<NearFieldTask>> {
        return queries.getAllTasks().asFlow().mapToList(Dispatchers.Default)
    }

    /**
     * 获取指定任务的所有签到人员
     */
    fun getParticipants(taskIdentifier: String): Flow<List<NearFieldParticipant>> {
        return queries.getParticipantsForTask(taskIdentifier).asFlow()
            .mapToList(Dispatchers.Default)
    }

    /**
     * 删除指定的签到任务及其所有参与者记录
     */
    suspend fun deleteTask(taskIdentifier: String) {
        withContext(Dispatchers.Default) {
            queries.deleteTask(taskIdentifier)
        }
    }

    /**
     * 删除单条参与者记录
     */
    suspend fun deleteParticipant(id: Long) {
        withContext(Dispatchers.Default) {
            queries.deleteParticipant(id)
        }
    }

    /**
     * 手动添加签到人员（用于补签或后台录入）
     */
    suspend fun manualAddParticipant(
        taskIdentifier: String,
        name: String,
        id: String,
        status: String
    ) {
        withContext(Dispatchers.Default) {
            queries.insertParticipant(
                taskIdentifier = taskIdentifier,
                participantName = name,
                participantId = id,
                checkinTime = com.suseoaa.projectoaa.shared.util.OaaClock.now()
                    .toEpochMilliseconds(),
                status = status
            )
        }
    }

    /**
     * 作为主机启动 TCP 服务器，接收学生的签到请求
     * 
     * @param port 监听端口
     * @param taskIdentifier 关联的任务标识符
     */
    fun startCheckinServer(port: Int, taskIdentifier: String) {
        serverJob?.cancel()
        serverJob = repositoryScope.launch {
            val selectorManager = SelectorManager(Dispatchers.Default)
            val serverSocket =
                aSocket(selectorManager).tcp().bind(hostname = "0.0.0.0", port = port)

            try {
                while (isActive) {
                    val socket = serverSocket.accept()
                    launch {
                        handleClient(socket, taskIdentifier)
                    }
                }
            } catch (e: Exception) {
                // 处理异常或记录日志
            } finally {
                serverSocket.close()
                selectorManager.close()
            }
        }
    }

    /**
     * 停止签到服务器
     */
    fun stopCheckinServer() {
        serverJob?.cancel()
        serverJob = null
    }

    /**
     * 处理客户端（学生端）发送的签到数据或同步请求
     */
    private suspend fun handleClient(socket: Socket, taskIdentifier: String) {
        val receiveChannel = socket.openReadChannel()
        val sendChannel = socket.openWriteChannel(autoFlush = true)
        try {
            val line = receiveChannel.readUTF8Line()
            if (line != null) {
                val message = json.decodeFromString<CheckinRequest>(line)

                var checkinError: String? = null
                val currentParticipants =
                    queries.getParticipantsForTask(taskIdentifier).executeAsList()

                if (message.type == "CHECKIN") {
                    val studentId = message.studentId ?: "000000"
                    val alreadyCheckedIn = currentParticipants.any { it.participantId == studentId }

                    if (alreadyCheckedIn) {
                        checkinError = "您已经签到过了"
                    } else {
                        queries.insertParticipant(
                            taskIdentifier = taskIdentifier,
                            participantName = message.studentName ?: "未知",
                            participantId = studentId,
                            checkinTime = message.timestamp,
                            status = message.status ?: "正常"
                        )
                    }
                }

                // 重新获取最新的完整名单
                val allParticipants = queries.getParticipantsForTask(taskIdentifier).executeAsList()
                val response = CheckinResponse(
                    success = checkinError == null,
                    errorMessage = checkinError,
                    participants = allParticipants.map {
                        ParticipantSyncItem(
                            it.participantName,
                            it.participantId,
                            it.checkinTime,
                            it.status
                        )
                    }
                )
                sendChannel.writeStringUtf8(json.encodeToString(response) + "\n")
            }
        } catch (e: Exception) {
            // 记录日志
        } finally {
            socket.close()
        }
    }

    /**
     * 学生端执行签到逻辑，通过 TCP 发送个人信息给主机，并接收同步回来的名单
     */
    suspend fun sendCheckinRequest(
        task: NearFieldCheckinTask,
        studentName: String,
        studentId: String,
        status: String = "正常"
    ): Result<Unit> {
        val host = task.hostAddress ?: return Result.failure(Exception("无法获取主机地址"))
        val port = task.hostPort ?: 8888

        return withContext(Dispatchers.Default) {
            val selectorManager = SelectorManager(Dispatchers.Default)
            try {
                val socket = aSocket(selectorManager).tcp().connect(host, port)
                val sendChannel = socket.openWriteChannel(autoFlush = true)
                val receiveChannel = socket.openReadChannel()

                val request = CheckinRequest(
                    type = "CHECKIN",
                    studentName = studentName,
                    studentId = studentId,
                    timestamp = com.suseoaa.projectoaa.shared.util.OaaClock.now()
                        .toEpochMilliseconds(),
                    status = status
                )

                sendChannel.writeStringUtf8(json.encodeToString(request) + "\n")

                // 读取回传的名单
                val responseLine = receiveChannel.readUTF8Line()
                var resultToReturn: Result<Unit> = Result.failure(Exception("未收到主机的响应"))
                if (responseLine != null) {
                    val response = json.decodeFromString<CheckinResponse>(responseLine)
                    if (response.success) {
                        saveTask(task, isMyHosted = false)
                        // 同步所有参与者到本地
                        response.participants.forEach { p ->
                            queries.insertParticipant(
                                taskIdentifier = task.taskIdentifier,
                                participantName = p.name,
                                participantId = p.id,
                                checkinTime = p.checkinTime,
                                status = p.status
                            )
                        }
                        resultToReturn = Result.success(Unit)
                    } else {
                        resultToReturn =
                            Result.failure(Exception(response.errorMessage ?: "签到被拒绝"))
                    }
                }

                socket.close()
                resultToReturn
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                selectorManager.close()
            }
        }
    }

    /**
     * 同步参与者名单（不进行签到，仅获取最新名单）
     */
    suspend fun syncParticipants(task: NearFieldCheckinTask): Result<Unit> {
        val host = task.hostAddress ?: return Result.failure(Exception("无法获取主机地址"))
        val port = task.hostPort ?: 8888

        return withContext(Dispatchers.Default) {
            val selectorManager = SelectorManager(Dispatchers.Default)
            try {
                val socket = aSocket(selectorManager).tcp().connect(host, port)
                val sendChannel = socket.openWriteChannel(autoFlush = true)
                val receiveChannel = socket.openReadChannel()

                val request = CheckinRequest(
                    type = "SYNC",
                    timestamp = com.suseoaa.projectoaa.shared.util.OaaClock.now()
                        .toEpochMilliseconds()
                )

                sendChannel.writeStringUtf8(json.encodeToString(request) + "\n")

                val responseLine = receiveChannel.readUTF8Line()
                if (responseLine != null) {
                    val response = json.decodeFromString<CheckinResponse>(responseLine)
                    if (response.success) {
                        response.participants.forEach { p ->
                            queries.insertParticipant(
                                taskIdentifier = task.taskIdentifier,
                                participantName = p.name,
                                participantId = p.id,
                                checkinTime = p.checkinTime,
                                status = p.status
                            )
                        }
                    }
                }

                socket.close()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                selectorManager.close()
            }
        }
    }
}

/**
 * 签到与同步请求模型
 */
@Serializable
data class CheckinRequest(
    val type: String, // "CHECKIN" 或 "SYNC"
    val studentName: String? = null,
    val studentId: String? = null,
    val timestamp: Long,
    val status: String? = "正常"
)

/**
 * 签到响应模型
 */
@Serializable
data class CheckinResponse(
    val success: Boolean,
    val errorMessage: String? = null,
    val participants: List<ParticipantSyncItem>
)

/**
 * 用于同步的参与者信息模型
 */
@Serializable
data class ParticipantSyncItem(
    val name: String,
    val id: String,
    val checkinTime: Long,
    val status: String
)
