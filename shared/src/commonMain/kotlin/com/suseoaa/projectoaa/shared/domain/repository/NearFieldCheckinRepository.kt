package com.suseoaa.projectoaa.shared.domain.repository

import com.suseoaa.projectoaa.shared.database.NearFieldParticipant
import com.suseoaa.projectoaa.shared.database.NearFieldTask
import com.suseoaa.projectoaa.shared.domain.nearfield.NearFieldCheckinTask
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * NearFieldCheckinRepository 的契约。
 *
 * 接口置于 domain 层、实现留在 data 层，让上层依赖抽象而非具体实现，
 * 测试中才能替换为假实现。
 */
interface NearFieldCheckinRepository {

    suspend fun saveTask(task: NearFieldCheckinTask, isMyHosted: Boolean): Unit

    fun getTaskHistory(): Flow<List<NearFieldTask>>

    fun getParticipants(taskIdentifier: String): Flow<List<NearFieldParticipant>>

    suspend fun deleteTask(taskIdentifier: String): Unit

    suspend fun deleteParticipant(id: Long): Unit

    suspend fun manualAddParticipant(
        taskIdentifier: String,
        name: String,
        id: String,
        status: String,
        ): Unit

    fun startCheckinServer(port: Int, taskIdentifier: String): Unit

    fun stopCheckinServer(): Unit

    suspend fun sendCheckinRequest(
        task: NearFieldCheckinTask,
        studentName: String,
        studentId: String,
        status: String = "正常",
    ): Result<Unit>

    suspend fun syncParticipants(task: NearFieldCheckinTask): Result<Unit>
}
