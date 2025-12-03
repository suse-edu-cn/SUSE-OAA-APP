package com.suseoaa.projectoaa.competition.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.suseoaa.projectoaa.common.base.BaseViewModel
import com.suseoaa.projectoaa.competition.model.MatchItem // (修改) 导入 MatchItem
import com.suseoaa.projectoaa.competition.model.MatchListUiItem
import com.suseoaa.projectoaa.competition.model.MatchStatus
import com.suseoaa.projectoaa.competition.repository.MatchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject

@HiltViewModel
class MatchListViewModel @Inject constructor(
    private val repository: MatchRepository
) : BaseViewModel() {

    var matchList by mutableStateOf<List<MatchListUiItem>>(emptyList())
        private set

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    init {
        fetchMatchList() // 初始化时加载
    }
    fun fetchMatchList() {
        launchDataLoad {
            // 1. 获取原始数据
            val rawList: List<MatchItem> = repository.getMatchList()

            // 2. 映射为 UI Item
            val uiList = rawList.map { item ->
                MatchListUiItem(
                    id = item.id,
                    title = item.title,
                    regTime = item.regTime, // (关键) 传递 regTime
                    matchTime = item.matchTime,
                    status = calculateMatchStatus(item.regTime, item.matchTime)
                )
            }

            // 3. 对列表进行排序
            matchList = uiList.sortedBy { statusToSortWeight(it.status) }
        }
    }

    /**
     * (新增) 为排序分配权重
     * (报名中 -> 准备 -> 比赛中 -> 结束)
     */
    private fun statusToSortWeight(status: MatchStatus): Int {
        return when (status) {
            MatchStatus.REGISTERING -> 1 // 1. 报名中
            MatchStatus.UPCOMING -> 2 // 2. 即将报名
            MatchStatus.REGISTRATION_ENDED -> 2 // 2. 准备比赛 (报名结束)
            MatchStatus.ONGOING -> 3 // 3. 比赛中
            MatchStatus.ENDED -> 4 // 4. 已结束
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