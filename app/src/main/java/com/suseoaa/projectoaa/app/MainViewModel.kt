package com.suseoaa.projectoaa.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.dataStore.TokenManager
import com.suseoaa.projectoaa.feature.home.MAIN_SCREEN_ROUTE
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(
    tokenManager: TokenManager
) : ViewModel() {
    val startDestination: StateFlow<String> = tokenManager.tokenFlow
        .map {
            if (it.isNullOrEmpty()) "login_route" else MAIN_SCREEN_ROUTE
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = "loading_route"
        )
}