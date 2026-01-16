package com.suseoaa.projectoaa.feature.academicPortal.getExamInfo

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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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

    // 2. 考试列表流 (含排序逻辑)
    @OptIn(ExperimentalCoroutinesApi::class)
    val examList: StateFlow<List<ExamUiState>> = tokenManager.currentStudentId
        .filterNotNull()
        .flatMapLatest { studentId ->
            repository.observeExams(studentId)
        }
        .map { entities ->
            val today = LocalDate.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

            entities.map { entity ->
                ExamUiState(
                    courseName = entity.courseName,
                    time = entity.time,
                    location = entity.location
                )
            }.sortedWith { a, b ->
                // 解析日期 helper
                val dateA = parseDate(a.time, formatter)
                val dateB = parseDate(b.time, formatter)

                // 异常处理：解析失败的项沉底
                if (dateA == null && dateB == null) return@sortedWith 0
                if (dateA == null) return@sortedWith 1
                if (dateB == null) return@sortedWith -1

                // 判断是否已结束（今天之前的算已结束）
                val isEndedA = dateA.isBefore(today)
                val isEndedB = dateB.isBefore(today)

                if (isEndedA != isEndedB) {
                    // 规则2:已结束的放到最后
                    // A已结束(true)，B未结束(false) -> A放后面(1)
                    if (isEndedA) 1 else -1
                } else {
                    // 规则1:状态相同时，按由先到后(时间升序)排列
                    // 这样未结束的考试中，离今天最近的排在最上面
                    dateA.compareTo(dateB)
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

    // 日期解析工具
    private fun parseDate(timeStr: String, formatter: DateTimeFormatter): LocalDate? {
        return try {
            // 格式为 "2026-01-08(09:30-11:30)"，截取括号前部分
            val datePart = timeStr.substringBefore("(")
            if (datePart.isBlank()) return null
            LocalDate.parse(datePart, formatter)
        } catch (e: Exception) {
            null
        }
    }
}