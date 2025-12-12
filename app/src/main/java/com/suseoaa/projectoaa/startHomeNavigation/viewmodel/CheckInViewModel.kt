package com.suseoaa.projectoaa.startHomeNavigation.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.common.util.WallpaperManager
import com.suseoaa.projectoaa.login.repository.AuthRepository
import com.suseoaa.projectoaa.startHomeNavigation.repository.UserDataRepository
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
    val placeholderImageUrl: Uri? = null,
    val isLoading: Boolean = false
)

/**
 * 专用于处理打卡相关的所有业务逻辑
 */
@HiltViewModel
class CheckInViewModel @Inject constructor(
    private val userDataRepository: UserDataRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    var uiState by mutableStateOf(CheckInUiState())
        private set

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)

            // 并行加载图片，不依赖 UserID，解决头图丢失问题
            val imageUriDeferred = withContext(Dispatchers.IO) {
                fetchDailyImage(LocalDate.now().toString())
            }

            // 尝试获取 student_id (即 userId)
            val userId = resolveUserId()

            // 根据 ID 读取本地打卡记录
            var isChecked = false
            var count = 0

            if (userId.isNotEmpty()) {
                val userData = withContext(Dispatchers.IO) {
                    val todayString = LocalDate.now().toString()

                    // 读取本地缓存的打卡数据
                    val storedDate = userDataRepository.getLastCheckInDate(userId)
                    val storedCount = userDataRepository.getCheckInCount(userId)

                    // TODO: 如果将来要从后端同步打卡数据，就在这里调用 API
                    // val remoteData = api.getCheckInStatus(userId) ...

                    Pair(todayString == storedDate, storedCount)
                }
                isChecked = userData.first
                count = userData.second
            } else {
                Log.w("CheckInVM", "未获取到 student_id，进入访客/未登录模式")
            }

            //更新 UI
            uiState = uiState.copy(
                isCheckedIn = isChecked,
                checkInCount = count,
                placeholderImageUrl = imageUriDeferred,
                isLoading = false
            )
        }
    }

    /**
     * 适配后端逻辑：如果没有 UserID，但有 Token，尝试调用 getUserInfo 恢复 student_id
     */
    private suspend fun resolveUserId(): String {
        // 优先从本地读取
        val localId = authRepository.getCurrentUserId()
        if (localId.isNotEmpty()) return localId

        // 如果本地丢失，尝试通过网络恢复 (针对更新后数据迁移或缓存被清的情况)
        val token = authRepository.getToken()
        if (!token.isNullOrEmpty()) {
            Log.d("CheckInVM", "尝试通过 Token 恢复 student_id...")
            val result = authRepository.getUserInfo(token)
            if (result.isSuccess) {
                // AuthRepository 内部已经保存了 student_id
                return authRepository.getCurrentUserId()
            }
        }
        return ""
    }

    /**
     * 图片获取逻辑：缓存优先 -> 网络获取 -> 存入缓存
     */
    private suspend fun fetchDailyImage(todayString: String): Uri? {
        return try {
            val (cachedDate, cachedUrl) = userDataRepository.getCachedImage()
            if (cachedDate == todayString && !cachedUrl.isNullOrEmpty()) {
                Uri.parse(cachedUrl)
            } else {
                val newUri = WallpaperManager.getCheckInImage(context)
                if (newUri != null) {
                    userDataRepository.saveCachedImage(todayString, newUri.toString())
                }
                newUri
            }
        } catch (e: Exception) {
            Log.e("CheckInVM", "Error loading image", e)
            null
        }
    }

    fun onCheckIn() {
        if (uiState.isCheckedIn) return

        viewModelScope.launch {
            val userId = resolveUserId() // 再次确认 ID
            if (userId.isEmpty()) {
                withContext(Dispatchers.Main){
                    Toast.makeText(context,"请先登录", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val todayString = LocalDate.now().toString()
            val newCount = uiState.checkInCount + 1

            // 1. 乐观更新 UI
            uiState = uiState.copy(isCheckedIn = true, checkInCount = newCount)

            // 2. 本地持久化 (Key 绑定 student_id)
            withContext(Dispatchers.IO) {
                userDataRepository.saveCheckInDate(userId, todayString)
                userDataRepository.saveCheckInCount(userId, newCount)

                // TODO: 预留后端接口
                // api.submitCheckIn(userId)
            }
        }
    }
}