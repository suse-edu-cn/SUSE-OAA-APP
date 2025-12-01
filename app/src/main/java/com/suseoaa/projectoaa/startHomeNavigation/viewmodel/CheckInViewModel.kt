package com.suseoaa.projectoaa.navigation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.common.util.WallpaperManager
import com.suseoaa.projectoaa.navigation.repository.UserDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

/**
 * 专用于打卡功能的 UI 状态
 */
data class CheckInUiState(
    val isCheckedIn: Boolean = false,
    val checkInCount: Int = 0,
    val placeholderImageUrl: Uri? = null
)

/**
 * 专用于处理打卡相关的所有业务逻辑
 */
@HiltViewModel
class CheckInViewModel @Inject constructor(
    private val userDataRepository: UserDataRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    var uiState by mutableStateOf(CheckInUiState())
        private set

    init {
        loadCheckInData()
    }

    private fun loadCheckInData() {
        viewModelScope.launch {
            // 1. (IO) 读取用户打卡数据
            val userData = withContext(Dispatchers.IO) {
                val todayString = LocalDate.now().toString()
                val storedDate = userDataRepository.getLastCheckInDate()
                val count = userDataRepository.getCheckInCount()
                Pair(todayString == storedDate, count)
            }

            // 2. (IO) 读取打卡图片
            val imageUri = withContext(Dispatchers.IO) {
                WallpaperManager.getCheckInImage(context)
            }

            // 3. 更新 UI 状态
            uiState = uiState.copy(
                isCheckedIn = userData.first,
                checkInCount = userData.second,
                placeholderImageUrl = imageUri
            )
        }
    }

    /**
     * 执行打卡操作
     */
    fun onCheckIn() {
        if (uiState.isCheckedIn) return
        viewModelScope.launch(Dispatchers.IO) {
            val todayString = LocalDate.now().toString()
            val newCount = uiState.checkInCount + 1
            userDataRepository.saveCheckInDate(todayString)
            userDataRepository.saveCheckInCount(newCount)

            withContext(Dispatchers.Main) {
                uiState = uiState.copy(isCheckedIn = true, checkInCount = newCount)
            }
        }
    }
}