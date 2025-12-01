package com.suseoaa.projectoaa.navigation.repository


interface WallpaperRepository {
    val currentWallpaper: kotlinx.coroutines.flow.Flow<android.net.Uri?>
    suspend fun saveCurrentToGallery()
    suspend fun refreshWallpaper()
}
