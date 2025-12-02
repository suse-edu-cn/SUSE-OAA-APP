package com.suseoaa.projectoaa.competition.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.suseoaa.projectoaa.common.base.BaseViewModel
import com.suseoaa.projectoaa.competition.model.MatchItem
import com.suseoaa.projectoaa.competition.repository.MatchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MatchListViewModel @Inject constructor(
    private val repository: MatchRepository
) : BaseViewModel() { // Hilt 继承 BaseViewModel

    var matchList by mutableStateOf<List<MatchItem>>(emptyList())
        private set

    init {
        fetchMatchList() // 初始化时加载
    }
    fun fetchMatchList() {
        launchDataLoad {
            matchList = repository.getMatchList()
        }
    }
}