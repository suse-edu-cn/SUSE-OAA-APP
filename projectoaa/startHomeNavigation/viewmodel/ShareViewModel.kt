package com.suseoaa.projectoaa.startHomeNavigation.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ShareViewModel : ViewModel() {
    private val _showOfState = MutableStateFlow(false)
    val showOfState: StateFlow<Boolean> = _showOfState

    // 预加载的页面数据（示例）
    private val _homeItems = MutableStateFlow<List<String>>(emptyList())
    val homeItems: StateFlow<List<String>> = _homeItems

    private val _searchHint = MutableStateFlow<String>("")
    val searchHint: StateFlow<String> = _searchHint

    private val _settingsInfo = MutableStateFlow<String>("")
    val settingsInfo: StateFlow<String> = _settingsInfo

    private val _profileInfo = MutableStateFlow<String>("")
    val profileInfo: StateFlow<String> = _profileInfo

    fun toggleOfUI() {
        _showOfState.value = !_showOfState.value
    }
}