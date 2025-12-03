package com.suseoaa.projectoaa.startHomeNavigation.repository


interface WallpaperRepository {
    val currentWallpaper: kotlinx.coroutines.flow.Flow<android.net.Uri?>
    suspend fun saveCurrentToGallery()
    suspend fun refreshWallpaper()
}
