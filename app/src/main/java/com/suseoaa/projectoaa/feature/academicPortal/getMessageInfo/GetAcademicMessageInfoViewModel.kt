package com.suseoaa.projectoaa.feature.academicPortal.getMessageInfo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.data.repository.SchoolInfoRepository
import com.suseoaa.projectoaa.core.database.dao.CourseDao
import com.suseoaa.projectoaa.core.dataStore.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GetAcademicMessageInfoViewModel @Inject constructor(
    private val repository: SchoolInfoRepository,
    private val tokenManager: TokenManager,
    private val courseDao: CourseDao
) : ViewModel() {

    // === 1. 数据流 (UI 自动更新) ===
    // 监听当前学号 -> 监听数据库中的消息表 -> 转换为 UI 需要的 List<String>
    @OptIn(ExperimentalCoroutinesApi::class)
    val dataList: StateFlow<List<String>> = tokenManager.currentStudentId
        .filterNotNull()
        .flatMapLatest { studentId ->
            repository.observeMessages(studentId)
        }
        .map { entities ->
            // 将实体转回 UI 层习惯的 String 列表
            entities.map { it.content }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    // === 2. 刷新状态控制 ===
    var isRefreshing by mutableStateOf(false)
        private set

    // 辅助：获取当前账号实体（用于网络请求鉴权）
    @OptIn(ExperimentalCoroutinesApi::class)
    private val currentAccount = tokenManager.currentStudentId
        .filterNotNull()
        .flatMapLatest { id -> flow { emit(courseDao.getAccountById(id)) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // === 3. 手动刷新逻辑 ===
    fun refreshData() {
        val account = currentAccount.value ?: return

        viewModelScope.launch {
            if (isRefreshing) return@launch
            isRefreshing = true

            // 调用仓库执行网络请求，成功后数据会自动写入数据库
            // 数据库变化会通过上面的 dataList Flow 自动通知 UI 更新
            val result = repository.refreshAcademicMessageInfo(account)

            isRefreshing = false
        }
    }
}