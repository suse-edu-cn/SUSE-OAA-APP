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

    // 1. 获取当前账号
    val currentAccount: StateFlow<CourseAccountEntity?> = localRepository.allAccounts
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // 智能计算默认学年
    private val defaultSelection: Pair<String, String>
        get() {
            val calendar = Calendar.getInstance()
            val month = calendar.get(Calendar.MONTH) + 1
            val year = calendar.get(Calendar.YEAR)

            return when (month) {
                1, 2 -> (year - 1).toString() to "3"
                in 3..7 -> (year - 1).toString() to "12"
                else -> year.toString() to "3"
            }
        }

    var selectedXnm by mutableStateOf(defaultSelection.first)
    var selectedXqm by mutableStateOf(defaultSelection.second)

    var isRefreshing by mutableStateOf(false)
        private set
    var refreshMessage by mutableStateOf<String?>(null)
        private set

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

    fun updateFilter(xnm: String, xqm: String) {
        selectedXnm = xnm
        selectedXqm = xqm
    }

    fun refreshGrades() {
        val account = currentAccount.value ?: return

        viewModelScope.launch {
            isRefreshing = true
            refreshMessage = "正在全量同步成绩..."

            try {
                val result = schoolRepository.fetchAllHistoryGrades(account)
                result.onSuccess { msg ->
                    refreshMessage = msg
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