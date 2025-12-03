package com.suseoaa.projectoaa.competition.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import com.suseoaa.projectoaa.common.base.BaseViewModel
// (修改) 导入 MatchDetail 和 MatchDetailUiItem
import com.suseoaa.projectoaa.competition.model.MatchDetail
import com.suseoaa.projectoaa.competition.model.MatchDetailUiItem
import com.suseoaa.projectoaa.competition.model.MatchStatus // (新增)
import com.suseoaa.projectoaa.competition.repository.MatchRepository
import com.suseoaa.projectoaa.startHomeNavigation.ui.MATCH_ID_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate // (新增)
import java.time.format.DateTimeFormatter // (新增)
import javax.inject.Inject

@HiltViewModel
class MatchDetailViewModel @Inject constructor(
    private val repository: MatchRepository,
    savedStateHandle: SavedStateHandle
) : BaseViewModel() {

    // 状态变量现在使用新的 UiItem
    var matchDetail by mutableStateOf<MatchDetailUiItem?>(null)
        private set

    private val matchId: Int = checkNotNull(savedStateHandle[MATCH_ID_ARG])

    // 从 MatchListViewModel 复制状态计算逻辑
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    init {
        fetchMatchDetail()
    }

    fun fetchMatchDetail() {
        launchDataLoad {
            // 1. 从仓库获取原始数据
            val rawDetail: MatchDetail = repository.getMatchDetail(matchId)

            // 2. 计算状态
            val status = calculateMatchStatus(rawDetail.regTime, rawDetail.conTime)

            // 3. 转换为 UI 模型
            matchDetail = MatchDetailUiItem(
                id = rawDetail.id,
                title = rawDetail.title,
                organizer = rawDetail.organizer,
                regTime = rawDetail.regTime,
                conTime = rawDetail.conTime,
                content = rawDetail.content,
                status = status // 传入计算好的状态
            )
        }
    }

    /**
     * 状态计算逻辑
     */
    private fun calculateMatchStatus(regTime: List<String>, matchTime: List<String>): MatchStatus {
        val today = LocalDate.now()
        try {
            val regStart = LocalDate.parse(regTime.getOrNull(0), dateFormatter)
            val regEnd = LocalDate.parse(regTime.getOrNull(1), dateFormatter)
            val matchStart = LocalDate.parse(matchTime.getOrNull(0), dateFormatter)
            val matchEnd = LocalDate.parse(matchTime.getOrNull(1), dateFormatter)

            return when {
                today.isAfter(matchEnd) -> MatchStatus.ENDED
                today.isAfter(matchStart.minusDays(1)) -> MatchStatus.ONGOING
                today.isAfter(regEnd) -> MatchStatus.REGISTRATION_ENDED
                today.isAfter(regStart.minusDays(1)) -> MatchStatus.REGISTERING
                else -> MatchStatus.UPCOMING
            }
        } catch (e: Exception) {
            return MatchStatus.UPCOMING
        }
    }
}