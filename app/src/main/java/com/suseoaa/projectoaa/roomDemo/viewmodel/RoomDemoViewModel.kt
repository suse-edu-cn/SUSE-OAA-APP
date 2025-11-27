package com.suseoaa.projectoaa.roomDemo.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.roomDemo.data.database.SampleDatabase
import com.suseoaa.projectoaa.roomDemo.data.entity.SampleEntity
import com.suseoaa.projectoaa.roomDemo.data.repository.SampleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Room数据库测试ViewModel
 * MVVM架构 - ViewModel层（管理UI状态和业务逻辑）
 */
class RoomDemoViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = SampleDatabase.getInstance(application)
    private val repository = SampleRepository(database)

    // UI状态
    var uiState by mutableStateOf(RoomDemoUiState())
        private set
    
    // 实时数据列表（使用StateFlow自动更新UI）
    val sampleList: StateFlow<List<SampleEntity>> = repository.getAllSamples()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    /**
     * 插入示例数据
     */
    fun insertSampleData() {
        viewModelScope.launch {
            try {
                uiState = uiState.copy(isLoading = true, errorMessage = null)
                repository.insertSampleData()
                uiState = uiState.copy(
                    isLoading = false,
                    successMessage = "成功插入示例数据"
                )
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = "插入失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 插入单条数据
     */
    fun insertSample(name: String, description: String) {
        viewModelScope.launch {
            try {
                if (name.isBlank()) {
                    uiState = uiState.copy(errorMessage = "名称不能为空")
                    return@launch
                }
                
                uiState = uiState.copy(isLoading = true, errorMessage = null)
                val sample = SampleEntity(name = name, description = description)
                repository.insertSample(sample)
                uiState = uiState.copy(
                    isLoading = false,
                    successMessage = "成功插入数据: $name"
                )
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = "插入失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 删除数据
     */
    fun deleteSample(sample: SampleEntity) {
        viewModelScope.launch {
            try {
                uiState = uiState.copy(isLoading = true, errorMessage = null)
                repository.deleteSample(sample)
                uiState = uiState.copy(
                    isLoading = false,
                    successMessage = "已删除: ${sample.name}"
                )
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = "删除失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 删除所有数据
     */
    fun deleteAllSamples() {
        viewModelScope.launch {
            try {
                uiState = uiState.copy(isLoading = true, errorMessage = null)
                repository.deleteAllSamples()
                uiState = uiState.copy(
                    isLoading = false,
                    successMessage = "已清空所有数据"
                )
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = "清空失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 更新数据
     */
    fun updateSample(sample: SampleEntity) {
        viewModelScope.launch {
            try {
                uiState = uiState.copy(isLoading = true, errorMessage = null)
                repository.updateSample(sample)
                uiState = uiState.copy(
                    isLoading = false,
                    successMessage = "已更新: ${sample.name}"
                )
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = "更新失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 获取数据统计
     */
    fun loadStatistics() {
        viewModelScope.launch {
            try {
                val count = repository.getSampleCount()
                uiState = uiState.copy(
                    totalCount = count,
                    successMessage = "当前共有 $count 条数据"
                )
            } catch (e: Exception) {
                uiState = uiState.copy(
                    errorMessage = "统计失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 清除消息
     */
    fun clearMessages() {
        uiState = uiState.copy(successMessage = null, errorMessage = null)
    }
}

/**
 * UI状态数据类
 */
data class RoomDemoUiState(
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val totalCount: Int = 0
)
