package com.suseoaa.projectoaa.navigation.viewmodel

import android.annotation.SuppressLint
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject

data class HomeUiState(
    val currentDate: String = "",
    val cspCountdown: String = "计算中...",
    val noipCountdown: String = "计算中..."
)

@HiltViewModel
class HomeViewModel @Inject constructor(
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiStateFlow = _uiState.asStateFlow()
    var uiState by mutableStateOf(HomeUiState())
        private set

    init {
        loadInitialData()
    }

    @SuppressLint("NewApi")
    private fun loadInitialData() {
        viewModelScope.launch {
            val dateInfo = withContext(Dispatchers.Default) {
                calculateDateAndCountdowns()
            }

            updateState {
                it.copy(
                    currentDate = dateInfo.first,
                    cspCountdown = dateInfo.second,
                    noipCountdown = dateInfo.third
                )
            }
        }
    }


    private fun updateState(update: (HomeUiState) -> HomeUiState) {
        val newState = update(_uiState.value)
        _uiState.value = newState
        uiState = newState
    }

    @SuppressLint("NewApi")
    private fun calculateDateAndCountdowns(): Triple<String, String, String> {
        val now = LocalDate.now()
        val dateFormatter = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINESE)
        val formattedDate = now.format(dateFormatter)
        val currentYear = now.year

        var targetCsp = LocalDate.of(currentYear, 9, 21)
        if (now.isAfter(targetCsp)) targetCsp = targetCsp.plusYears(1)

        var targetNoip = LocalDate.of(currentYear, 11, 30)
        if (now.isAfter(targetNoip)) targetNoip = targetNoip.plusYears(1)

        val cspDays = ChronoUnit.DAYS.between(now, targetCsp)
        val noipDays = ChronoUnit.DAYS.between(now, targetNoip)

        return Triple(
            formattedDate,
            "距 CSP-J/S ${targetCsp.year} 还剩 $cspDays 天",
            "距 NOIP ${targetNoip.year} 还剩 $noipDays 天"
        )
    }
}