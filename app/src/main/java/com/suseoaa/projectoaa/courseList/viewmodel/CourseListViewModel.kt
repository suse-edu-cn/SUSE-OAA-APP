package com.suseoaa.projectoaa.courseList.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.courseList.data.database.CourseDatabase
import com.suseoaa.projectoaa.courseList.data.repository.CourseRepository
import com.suseoaa.projectoaa.courseList.data.remote.SchoolSystem
import com.suseoaa.projectoaa.courseList.data.remote.dto.CourseResponseJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 课表ViewModel
 * MVVM架构 - ViewModel层（管理UI状态和业务逻辑）
 */
class CourseListViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = CourseDatabase.getInstance(application)
    private val repository = CourseRepository(database.courseDao())
    
    // UI状态
    var uiState by mutableStateOf(CourseListUiState())
        private set
    
    // 课程数据
    private val _courseData: MutableStateFlow<CourseResponseJson?> = MutableStateFlow(null)
    val courseData: StateFlow<CourseResponseJson?> = _courseData.asStateFlow()
    
    /**
     * 从学校系统获取课表并保存
     */
    fun fetchAndSaveCourseSchedule(username: String, password: String) {
        viewModelScope.launch {
            try {
                uiState = uiState.copy(
                    isLoading = true,
                    errorMessage = null,
                    statusMessage = "正在登录..."
                )
                
                val result = withContext(Dispatchers.IO) {
                    // 登录
                    val (loginSuccess, debugInfo) = SchoolSystem.login(username, password)
                    
                    if (!loginSuccess) {
                        return@withContext Triple(null, "登录失败: $debugInfo", debugInfo)
                    }
                    
                    uiState = uiState.copy(statusMessage = "正在获取课表...")
                    
                    // 获取课表数据
                    val (parsedData, scheduleDebugInfo) = SchoolSystem.queryScheduleParsed()
                    
                    if (parsedData == null) {
                        return@withContext Triple(null, "课表数据解析失败: $scheduleDebugInfo", scheduleDebugInfo)
                    }
                    
                    Triple(parsedData, null, "成功获取课表")
                }
                
                val (courseData, error, _) = result

                if (courseData != null) {
                    // 保存到数据库
                    uiState = uiState.copy(statusMessage = "正在保存到数据库...")
                    withContext(Dispatchers.IO) {
                        repository.saveFromResponse(courseData)
                    }
                    
                    _courseData.value = courseData
                    uiState = uiState.copy(
                        isLoading = false,
                        successMessage = "成功获取并保存课表数据",
                        statusMessage = null
                    )
                } else {
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage = error,
                        statusMessage = null
                    )
                }
                
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = "发生异常: ${e.message}",
                    statusMessage = null
                )
            }
        }
    }
    
    /**
     * 清除消息
     */
    fun clearMessages() {
        uiState = uiState.copy(
            successMessage = null,
            errorMessage = null,
            statusMessage = null
        )
    }
}

/**
 * 课表UI状态
 */
data class CourseListUiState(
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val statusMessage: String? = null
)
