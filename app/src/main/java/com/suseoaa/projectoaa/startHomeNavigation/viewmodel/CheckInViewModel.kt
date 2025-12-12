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
    val isLoading: Boolean = false,
    val dailyFortune: DailyFortune? = null
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
            val userId = resolveUserId()
            val imageUriDeferred = withContext(Dispatchers.IO) {
                fetchDailyImage(LocalDate.now().toString())
            }

            // [TODO] API对接: 替换为 "网络优先，本地降级" 策略
            // 修改建议如下：
            /*
            val fortune = withContext(Dispatchers.IO) {
                // 1. 调用 Repository 获取 (Repository 负责处理 API -> DTO -> Domain Model 的转换)
                // val result = userDataRepository.getDailyFortune(userId)

                // 2. 处理 Result: 如果网络成功用网络数据，失败则回退到本地逻辑
                // result.getOrElse { e ->
                //    Log.w("CheckInVM", "API请求失败，降级为本地算法: ${e.message}")
                //    FortuneLogic.generateFortuneForToday(userId)
                // }
            }
            */

            // 当前逻辑（纯本地）：
            val fortune = withContext(Dispatchers.Default) {
                FortuneLogic.generateFortuneForToday(userId)
            }

            var isChecked = false
            var count = 0

            if (userId.isNotEmpty()) {
                val userData = withContext(Dispatchers.IO) {
                    val todayString = LocalDate.now().toString()

                    // 读取本地缓存的打卡数据
                    val storedDate = userDataRepository.getLastCheckInDate(userId)
                    val storedCount = userDataRepository.getCheckInCount(userId)

                    // [TODO] API对接: 调用 API 获取服务端最新的打卡状态
                    // 用于防止用户换手机后打卡记录丢失
                    // val remoteStatus = apiService.getCheckInStatus(userId)
                    // if (remoteStatus.isSuccessful) { ... 更新本地 storedDate 和 storedCount ... }

                    Pair(todayString == storedDate, storedCount)
                }
                isChecked = userData.first
                count = userData.second
            } else {
                Log.w("CheckInVM", "未获取到 student_id，进入访客/未登录模式")
            }

            // 更新 UI
            uiState = uiState.copy(
                isCheckedIn = isChecked,
                checkInCount = count,
                placeholderImageUrl = imageUriDeferred,
                isLoading = false,
                dailyFortune = fortune
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

                // [TODO] API对接: 调用 API 提交打卡
                // try {
                //     val request = CheckInRequest(userId = userId, date = todayString)
                //     val response = apiService.submitCheckIn(request)
                //     if (!response.isSuccess) {
                //         // 处理提交失败的情况，例如加入本地待重试队列 WorkManager
                //     }
                // } catch (e: Exception) {
                //     Log.e("CheckInVM", "打卡上传失败", e)
                // }
            }
        }
    }
}