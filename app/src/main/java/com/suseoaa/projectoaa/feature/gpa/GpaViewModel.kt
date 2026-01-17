package com.suseoaa.projectoaa.feature.gpa

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.core.database.dao.CourseDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortOrder {
    DESCENDING, // 从高到低
    ASCENDING   // 从低到高
}

enum class FilterType {
    ALL,        // 全部课程
    DEGREE_ONLY // 仅学位课
}

data class GpaStats(
    val totalGpa: String = "0.00",
    val degreeGpa: String = "0.00",
    val totalCredits: String = "0.0",
    val degreeCredits: String = "0.0"
)

@HiltViewModel
class GpaViewModel @Inject constructor(
    private val gpaRepository: GpaRepository,
    private val tokenManager: TokenManager,
    private val courseDao: CourseDao
) : ViewModel() {

    // 数据源（保存所有课程）
    private var allCourses: List<GpaCourseWrapper> = emptyList()

    // UI 展示用的列表
    private val _courseList = MutableStateFlow<List<GpaCourseWrapper>>(emptyList())
    val courseList = _courseList.asStateFlow()

    private val _stats = MutableStateFlow(GpaStats())
    val stats = _stats.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DESCENDING)
    val sortOrder = _sortOrder.asStateFlow()

    private val _filterType = MutableStateFlow(FilterType.ALL)
    val filterType = _filterType.asStateFlow()

    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    fun loadData() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                // 1. 获取当前登录学号
                val studentId = tokenManager.currentStudentId.first()

                if (studentId.isNullOrEmpty()) {
                    errorMessage = "未找到学生信息，请先登录"
                    isLoading = false
                    return@launch
                }

                // 2. === 从数据库获取当前用户的详细信息 ===
                // 这样切换账号时，读取的就是对应账号的 jgId/zyhId，重启 App 后也不会丢失
                val account = courseDao.getAccount(studentId)

                if (account == null) {
                    errorMessage = "账户信息不存在，请重新登录"
                    isLoading = false
                    return@launch
                }

                val jgId = account.jgId
                val njdmId = account.njdmId
                val zyhId = account.zyhId

                // 如果数据库里没有 jgId，说明用户还没刷新过成绩
                if (jgId.isNullOrEmpty()) {
                    errorMessage = "缺少专业信息，请先在【成绩查询】页面下拉刷新一次以同步数据"
                    isLoading = false
                    return@launch
                }

                // 3. 调用 Repository 获取数据
                val result = gpaRepository.getGpaData(studentId, jgId, njdmId, zyhId)

                result.onSuccess { list ->
                    // 初始化 GPA (优先用数据库存的 GPA，没有则计算)
                    val initializedList = list.map { item ->
                        val originalGpa = item.originalEntity.gpa.toDoubleOrNull()
                            ?: calculateSingleGpa(item.simulatedScore)
                        item.copy(simulatedGpa = originalGpa)
                    }
                    allCourses = initializedList
                    applyFilterAndSort()
                }.onFailure { e ->
                    errorMessage = "数据获取失败: ${e.message}"
                }

            } catch (e: Exception) {
                errorMessage = "发生错误: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
        applyFilterAndSort()
    }

    fun setFilterType(type: FilterType) {
        _filterType.value = type
        applyFilterAndSort()
    }

    private fun applyFilterAndSort() {
        var list = allCourses

        // 1. 筛选
        if (_filterType.value == FilterType.DEGREE_ONLY) {
            list = list.filter { it.isDegreeCourse }
        }

        // 2. 排序
        list = when (_sortOrder.value) {
            SortOrder.DESCENDING -> list.sortedByDescending { it.simulatedScore }
            SortOrder.ASCENDING -> list.sortedBy { it.simulatedScore }
        }

        // 3. 更新 UI
        _courseList.value = list
        calculateTotalStats(list)
    }

    fun updateSimulatedScore(course: GpaCourseWrapper, newScore: Double) {
        val index = allCourses.indexOfFirst { it.originalEntity.courseId == course.originalEntity.courseId }

        if (index != -1) {
            val newGpa = calculateSingleGpa(newScore)

            val updatedItem = allCourses[index].copy(
                simulatedScore = newScore,
                simulatedGpa = newGpa
            )

            val newAllCourses = allCourses.toMutableList()
            newAllCourses[index] = updatedItem
            allCourses = newAllCourses

            applyFilterAndSort()
        }
    }

    private fun calculateSingleGpa(score: Double): Double {
        return when {
            score >= 95.0 -> 4.5
            score < 60.0 -> 0.0
            else -> {
                val base = 1.0
                val steps = ((score - 60) / 5).toInt()
                base + steps * 0.5
            }
        }
    }

    private fun calculateTotalStats(list: List<GpaCourseWrapper>) {
        var totalPoints = 0.0
        var totalCredits = 0.0

        var degreePoints = 0.0
        var degreeCredits = 0.0

        list.forEach { item ->
            val credit = item.originalEntity.credit.toDoubleOrNull() ?: 0.0
            val scoreStr = item.originalEntity.score.trim()

            // 排除 "合格", "通过", "缓考", "免修" 等
            val isExcluded = listOf("合格", "通过", "缓考", "免修").any { scoreStr.contains(it) }

            if (credit > 0.0 && !isExcluded) {
                totalPoints += item.simulatedGpa * credit
                totalCredits += credit

                if (item.isDegreeCourse) {
                    degreePoints += item.simulatedGpa * credit
                    degreeCredits += credit
                }
            }
        }

        val finalTotalGpa = if (totalCredits > 0) totalPoints / totalCredits else 0.0
        val finalDegreeGpa = if (degreeCredits > 0) degreePoints / degreeCredits else 0.0

        _stats.value = GpaStats(
            totalGpa = "%.2f".format(finalTotalGpa),
            degreeGpa = "%.2f".format(finalDegreeGpa),
            totalCredits = "%.1f".format(totalCredits),
            degreeCredits = "%.1f".format(degreeCredits)
        )
    }
}