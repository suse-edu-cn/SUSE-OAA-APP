package com.suseoaa.projectoaa.feature.academicPortal.getExamInfo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.data.repository.SchoolInfoRepository
import com.suseoaa.projectoaa.core.database.dao.CourseDao
import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.core.util.parseExamTimeRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class GetExamInfoViewModel @Inject constructor(
    private val repository: SchoolInfoRepository,
    private val courseDao: CourseDao,
    tokenManager: TokenManager
) : ViewModel() {

    // 1. 获取当前账户 (用于网络请求鉴权)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val currentAccount = tokenManager.currentStudentId
        .filterNotNull()
        .flatMapLatest { id -> flow { emit(courseDao.getAccountById(id)) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // 2. 考试列表流 (含精确排序逻辑)
    @OptIn(ExperimentalCoroutinesApi::class)
    val examList: StateFlow<List<ExamUiState>> = tokenManager.currentStudentId
        .filterNotNull()
        .flatMapLatest { studentId ->
            repository.observeExams(studentId)
        }
        .map { entities ->
            val now = LocalDateTime.now()

            entities.map { entity ->
                ExamUiState(
                    courseName = entity.courseName,
                    time = entity.time,
                    location = entity.location
                )
            }.sortedWith { a, b ->
                // 使用工具类解析完整时间
                val timesA = parseExamTimeRange(a.time)
                val timesB = parseExamTimeRange(b.time)

                // 异常处理：解析失败的项放到最后
                if (timesA == null && timesB == null) return@sortedWith 0
                if (timesA == null) return@sortedWith 1
                if (timesB == null) return@sortedWith -1

                val (startA, endA) = timesA
                val (startB, endB) = timesB

                // 判断是否已结束 (当前时间 > 结束时间)
                val isEndedA = now.isAfter(endA)
                val isEndedB = now.isAfter(endB)

                if (isEndedA != isEndedB) {
                    // 规则1: 状态不同时，未结束的在前，已结束的在后
                    // A已结束(true) -> 放后面(1)
                    if (isEndedA) 1 else -1
                } else {
                    // 规则2: 状态相同时（都未结束 或 都已结束）
                    // 按开始时间升序排列（离现在最近的/最早发生的排前面）
                    // 这样同一天不同时间的考试会按时间顺序排列
                    startA.compareTo(startB)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 3. 刷新状态
    var isRefreshing by mutableStateOf(false)
        private set

    // 4. 手动刷新
    fun refreshData() {
        val account = currentAccount.value ?: return
        viewModelScope.launch {
            if (isRefreshing) return@launch
            isRefreshing = true
            repository.refreshAcademicExamInfo(account)
            isRefreshing = false
        }
    }
}