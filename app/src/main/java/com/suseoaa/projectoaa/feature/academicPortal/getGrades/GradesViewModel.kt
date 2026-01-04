package com.suseoaa.projectoaa.feature.academicPortal.getGrades

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.data.repository.CourseRepository
import com.suseoaa.projectoaa.core.data.repository.SchoolRepository
import com.suseoaa.projectoaa.core.database.entity.CourseAccountEntity
import com.suseoaa.projectoaa.core.database.entity.GradeEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class GradesViewModel @Inject constructor(
    private val schoolRepository: SchoolRepository,
    private val localRepository: CourseRepository
) : ViewModel() {

    // 1. 获取当前账号 (从数据库流式获取)
    val currentAccount: StateFlow<CourseAccountEntity?> = localRepository.allAccounts
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // 2. UI 筛选状态
    var selectedXnm by mutableStateOf(Calendar.getInstance().get(Calendar.YEAR).toString())
    var selectedXqm by mutableStateOf("3") // 默认上学期

    // 3. 刷新状态与消息
    var isRefreshing by mutableStateOf(false)
        private set
    var refreshMessage by mutableStateOf<String?>(null)
        private set

    // 4. [核心] 数据流：观察数据库
    // 只要 账号 或 年份 或 学期 变化，Flow 就会自动切换查询
    @OptIn(ExperimentalCoroutinesApi::class)
    val grades: StateFlow<List<GradeEntity>> = combine(
        currentAccount.filterNotNull(),
        snapshotFlow { selectedXnm },
        snapshotFlow { selectedXqm }
    ) { account, xnm, xqm ->
        Triple(account.studentId, xnm, xqm)
    }.flatMapLatest { (studentId, xnm, xqm) ->
        schoolRepository.observeGrades(studentId, xnm, xqm)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    // 更新筛选条件 (纯 UI 动作，不触发网络)
    fun updateFilter(xnm: String, xqm: String) {
        selectedXnm = xnm
        selectedXqm = xqm
    }

    // [核心] 刷新所有历史成绩
    fun refreshGrades() {
        val account = currentAccount.value ?: return

        viewModelScope.launch {
            isRefreshing = true
            refreshMessage = "正在全量同步成绩..."

            try {
                // 调用 Repository 的批量同步
                val result = schoolRepository.fetchAllHistoryGrades(account)

                result.onSuccess { msg ->
                    refreshMessage = msg // 显示成功消息
                }.onFailure { e ->
                    refreshMessage = "更新失败: ${e.message}"
                }
            } catch (e: Exception) {
                refreshMessage = "未知错误: ${e.message}"
            } finally {
                isRefreshing = false
            }
        }
    }

    fun clearMessage() {
        refreshMessage = null
    }
}