package com.suseoaa.projectoaa.util

import kotlinx.coroutines.flow.MutableStateFlow

object DeepLinkManager {
    val pendingDeepLink = MutableStateFlow<String?>(null)
    
    fun consume() {
        pendingDeepLink.value = null
    }
}
