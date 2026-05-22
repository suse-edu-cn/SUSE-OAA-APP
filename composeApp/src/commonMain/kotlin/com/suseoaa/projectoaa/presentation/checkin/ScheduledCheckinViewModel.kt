package com.suseoaa.projectoaa.presentation.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.scheduling.PlatformCheckinScheduler
import com.suseoaa.projectoaa.shared.data.repository.CheckinRepository
import com.suseoaa.projectoaa.shared.domain.model.checkin.CheckinAccountData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 定时签到弹窗 UI 状态
 */
data class ScheduledCheckinUiState(
    val config: SchedulerConfig = SchedulerConfig(),
    val availableAccounts: List<CheckinAccountData> = emptyList(),
    val selectedAccountIds: Set<Long> = emptySet(),
    val schedulerStatus: SchedulerStatus = SchedulerStatus.Idle,
    val showDialog: Boolean = false,
    val isSaving: Boolean = false
)

/**
 * 定时签到 ViewModel
 */
class ScheduledCheckinViewModel(
    private val scheduledCheckinManager: ScheduledCheckinManager,
    private val checkinRepository: CheckinRepository,
    private val checkinScheduler: CheckinScheduler,
    private val platformCheckinScheduler: PlatformCheckinScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduledCheckinUiState())
    val uiState: StateFlow<ScheduledCheckinUiState> = _uiState.asStateFlow()

    init {
        // 收集调度器状态
        viewModelScope.launch {
            checkinScheduler.status.collectLatest { status ->
                _uiState.update { it.copy(schedulerStatus = status) }
            }
        }
    }

    /**
     * 显示弹窗，加载配置和账号
     */
    fun show() {
        viewModelScope.launch {
            val config = scheduledCheckinManager.getConfig()
            val allAccounts = checkinRepository.getAllAccounts()
            val passwordAccounts = allAccounts.filter { !it.isQrCodeLogin }

            _uiState.update {
                it.copy(
                    config = config,
                    availableAccounts = passwordAccounts,
                    selectedAccountIds = config.targetAccountIds.toSet(),
                    showDialog = true
                )
            }
        }
    }

    /**
     * 关闭弹窗
     */
    fun dismiss() {
        _uiState.update { it.copy(showDialog = false) }
    }

    /**
     * 切换账号选中状态
     */
    fun toggleAccount(accountId: Long) {
        _uiState.update { state ->
            val newIds = if (accountId in state.selectedAccountIds) {
                state.selectedAccountIds - accountId
            } else {
                state.selectedAccountIds + accountId
            }
            state.copy(selectedAccountIds = newIds)
        }
    }

    /**
     * 设置签到小时
     */
    fun setHour(hour: Int) {
        _uiState.update {
            it.copy(config = it.config.copy(scheduledHour = hour.coerceIn(0, 23)))
        }
    }

    /**
     * 设置签到分钟
     */
    fun setMinute(minute: Int) {
        _uiState.update {
            it.copy(config = it.config.copy(scheduledMinute = minute.coerceIn(0, 59)))
        }
    }

    /**
     * 设置签到秒数
     */
    fun setSecond(second: Int) {
        _uiState.update {
            it.copy(config = it.config.copy(scheduledSecond = second.coerceIn(0, 59)))
        }
    }

    /**
     * 设置重试次数
     */
    fun setRetryCount(count: Int) {
        _uiState.update {
            it.copy(config = it.config.copy(maxRetryCount = count.coerceIn(0, 10)))
        }
    }

    /**
     * 设置重试间隔（分钟）
     */
    fun setRetryInterval(minutes: Int) {
        _uiState.update {
            it.copy(config = it.config.copy(retryIntervalMinutes = minutes.coerceIn(1, 60)))
        }
    }

    /**
     * 切换启用状态
     */
    fun toggleEnabled() {
        _uiState.update {
            it.copy(config = it.config.copy(enabled = !it.config.enabled))
        }
    }

    /**
     * 保存配置并启动/停止调度器
     */
    fun saveConfig() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val state = _uiState.value
            val config = SchedulerConfig(
                enabled = state.config.enabled,
                targetAccountIds = state.selectedAccountIds.toList(),
                scheduledHour = state.config.scheduledHour,
                scheduledMinute = state.config.scheduledMinute,
                scheduledSecond = state.config.scheduledSecond,
                maxRetryCount = state.config.maxRetryCount,
                retryIntervalMinutes = state.config.retryIntervalMinutes,
                lastRunTimestamp = state.config.lastRunTimestamp,
                lastRunResult = state.config.lastRunResult,
                lastRunDate = state.config.lastRunDate
            )

            scheduledCheckinManager.saveConfig(config)

            if (config.enabled && config.targetAccountIds.isNotEmpty()) {
                checkinScheduler.start()
                platformCheckinScheduler.schedule(config)
            } else {
                checkinScheduler.stop()
                platformCheckinScheduler.cancel()
            }

            _uiState.update { it.copy(isSaving = false, showDialog = false) }
        }
    }
}
