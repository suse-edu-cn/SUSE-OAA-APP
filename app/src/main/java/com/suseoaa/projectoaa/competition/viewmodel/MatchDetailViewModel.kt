package com.suseoaa.projectoaa.competition.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import com.suseoaa.projectoaa.common.base.BaseViewModel
import com.suseoaa.projectoaa.competition.model.MatchDetail
import com.suseoaa.projectoaa.competition.model.MatchDetailUiItem
import com.suseoaa.projectoaa.competition.model.MatchStatus
import com.suseoaa.projectoaa.competition.repository.MatchRepository
import com.suseoaa.projectoaa.startHomeNavigation.ui.MATCH_ID_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MatchDetailViewModel @Inject constructor(
    private val repository: MatchRepository,
    savedStateHandle: SavedStateHandle
) : BaseViewModel() {

    var matchDetail by mutableStateOf<MatchDetailUiItem?>(null)
        private set

    private val matchId: Int = checkNotNull(savedStateHandle[MATCH_ID_ARG])

    init {
        fetchMatchDetail()
    }

    fun fetchMatchDetail() {
        launchDataLoad {
            //原始数据
            val rawDetail: MatchDetail = repository.getMatchDetail(matchId)

            //转换状态
            val statusEnum = MatchStatus.fromInt(rawDetail.status)

            //转换为 UI 模型
            matchDetail = MatchDetailUiItem(
                id = matchId,
                title = rawDetail.title,
                organizerName = "${rawDetail.author.name}",
                regTime = rawDetail.regTime,
                matchTime = rawDetail.matchTime,
                content = rawDetail.content,
                status = statusEnum
            )
        }
    }
}