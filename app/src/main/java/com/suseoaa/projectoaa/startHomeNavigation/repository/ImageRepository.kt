package com.suseoaa.projectoaa.navigation.repository

sealed class ImageResult {
    data class Success(val url: String) : ImageResult()
    data class Error(val exception: Exception) : ImageResult()
}

interface ImageRepository {
    // suspend 只能在协程中调用
    suspend fun fetchPlaceholderImage(): ImageResult
}