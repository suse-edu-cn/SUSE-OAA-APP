package com.suseoaa.projectoaa.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.core.data.repository.AnnouncementRepository
import com.suseoaa.projectoaa.core.network.model.announcement.FetchAnnouncementInfoResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AnnouncementRepository
) : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage = _toastMessage.asStateFlow()

    private val _announcementInfo = MutableStateFlow<FetchAnnouncementInfoResponse.Data?>(null)
    val announcementInfo = _announcementInfo.asStateFlow()

    init {
        fetchInfo("协会")
    }

    fun fetchInfo(department: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.fetchAnnouncementInfo(department)
            result.onSuccess {
                _announcementInfo.value = it
            }
            _isLoading.value = false
        }
    }
}