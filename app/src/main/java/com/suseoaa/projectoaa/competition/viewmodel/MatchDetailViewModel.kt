package com.suseoaa.projectoaa.competition.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import com.suseoaa.projectoaa.common.base.BaseViewModel
import com.suseoaa.projectoaa.competition.model.MatchDetail
import com.suseoaa.projectoaa.competition.repository.MatchRepository
import com.suseoaa.projectoaa.startHomeNavigation.ui.MATCH_ID_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MatchDetailViewModel @Inject constructor(
    private val repository: MatchRepository,
    savedStateHandle: SavedStateHandle // Hilt 注入，用于获取导航参数
) : BaseViewModel() {

    var matchDetail by mutableStateOf<MatchDetail?>(null)
        private set

    private val matchId: Int = checkNotNull(savedStateHandle[MATCH_ID_ARG])

    init {
        fetchMatchDetail()
    }

    fun fetchMatchDetail() {
        launchDataLoad {
            matchDetail = repository.getMatchDetail(matchId)
        }
    }
}