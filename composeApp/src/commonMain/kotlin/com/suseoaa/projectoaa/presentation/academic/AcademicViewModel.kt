package com.suseoaa.projectoaa.presentation.academic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.data.repository.LocalCourseRepository
import com.suseoaa.projectoaa.data.repository.SchoolAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 考试响应
 */
@Serializable
data class ExamResponse(
    @SerialName("items")
    val items: List<ExamItem>? = emptyList(),
    @SerialName("totalResult")
    val totalResult: Int? = 0,
    @SerialName("currentPage")
    val currentPage: Int? = 1
)

/**
 * 考试信息条目 - 匹配教务系统返回的字段
 */
@Serializable
data class ExamItem(
    @SerialName("kcmc")
    val kcmc: String? = "",   // 课程名称: "网络安全技术"
    @SerialName("kssj")
    val kssj: String? = "",   // 考试时间: "2026-01-08(09:30-11:30)"
    @SerialName("cdmc")
    val cdmc: String? = "",   // 教室名称: "LA5-322"
    @SerialName("cdxqmc")
    val cdxqmc: String? = "", // 校区: "临港校区"
    @SerialName("zw")
    val zw: String? = "",     // 座位号
    @SerialName("xh")
    val xh: String? = "",     // 学号
    @SerialName("xm")
    val xm: String? = ""      // 姓名
)

data class AcademicUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val messages: List<String> = emptyList(),
    val exams: List<ExamItem> = emptyList(),
    val errorMessage: String? = null
)

class AcademicViewModel(
    private val tokenManager: TokenManager,
    private val localCourseRepository: LocalCourseRepository,
    private val schoolAuthRepository: SchoolAuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AcademicUiState())
    val uiState: StateFlow<AcademicUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            // TODO: 从 SchoolInfoRepository 加载考试数据
            // 需要实现 SchoolInfoRepository 来获取考试信息
            
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    errorMessage = "教务系统考试信息功能正在开发中"
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            
            // TODO: 刷新考试数据
            
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
