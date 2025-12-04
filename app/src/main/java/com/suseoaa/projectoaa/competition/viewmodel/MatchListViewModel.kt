package com.suseoaa.projectoaa.competition.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.suseoaa.projectoaa.common.base.BaseViewModel
import com.suseoaa.projectoaa.competition.model.MatchItem
import com.suseoaa.projectoaa.competition.model.MatchListUiItem
import com.suseoaa.projectoaa.competition.model.MatchStatus
import com.suseoaa.projectoaa.competition.repository.MatchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MatchListViewModel @Inject constructor(
    private val repository: MatchRepository
) : BaseViewModel() {

    var matchList by mutableStateOf<List<MatchListUiItem>>(emptyList())
        private set

    init {
        fetchMatchList()
    }

    fun fetchMatchList() {
        launchDataLoad {
            // 获取原始数据列表
            val rawList: List<MatchItem> = repository.getMatchList()

            val uiList = rawList.map { item ->
                MatchListUiItem(
                    id = item.id,
                    title = item.title,
                    regTime = item.regTime,
                    matchTime = item.matchTime,
                    // 修复点：这里必须调用 item.status (Int)，而不是 item (MatchItem)
                    // 注意：请确保你的 MatchItem 数据类中已经添加了 val status: Int 字段
                    status = MatchStatus.fromInt(item.status)
                )
            }

            // 根据状态排序
            matchList = uiList.sortedBy { statusToSortWeight(it.status) }
        }
    }

    /**
     * 定义排序权重
     */
    private fun statusToSortWeight(status: MatchStatus): Int {
        return when (status) {
            MatchStatus.REGISTERING -> 1       // 报名中
            MatchStatus.UPCOMING -> 2          // 筹备中
            MatchStatus.REGISTRATION_ENDED -> 2 // 即将比赛 (和筹备中权重一样，按时间自然排序)
            MatchStatus.ONGOING -> 3           // 比赛中
            MatchStatus.ENDED -> 4             // 已结束
        }
    }
}