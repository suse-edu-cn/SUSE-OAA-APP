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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class MatchListViewModel @Inject constructor(
    private val repository: MatchRepository
) : BaseViewModel() {

    var matchList by mutableStateOf<List<MatchListUiItem>>(emptyList())
        private set

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    init {
        fetchMatchList()
    }

    fun fetchMatchList() {
        launchDataLoad {
            val rawList: List<MatchItem> = repository.getMatchList()

            val uiList = rawList.map { item ->
                MatchListUiItem(
                    id = item.id,
                    title = item.title,
                    regTime = item.regTime,
                    matchTime = item.matchTime,
                    // 列表接口没返回 status，依然需要本地计算
                    status = calculateMatchStatus(item.regTime, item.matchTime)
                )
            }

            matchList = uiList.sortedBy { statusToSortWeight(it.status) }
        }
    }

    private fun statusToSortWeight(status: MatchStatus): Int {
        return when (status) {
            MatchStatus.REGISTERING -> 1
            MatchStatus.UPCOMING -> 2
            MatchStatus.REGISTRATION_ENDED -> 2
            MatchStatus.ONGOING -> 3
            MatchStatus.ENDED -> 4
        }
    }

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