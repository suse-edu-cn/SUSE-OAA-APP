package com.suseoaa.projectoaa.navigation.repository

import android.content.Context
import android.net.Uri
import com.suseoaa.projectoaa.common.util.WallpaperManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WallpaperRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : WallpaperRepository {

    // [新增] 这里的 Flow 会自动通知 UI 数据变了
    override val currentWallpaper: Flow<Uri?> = WallpaperManager.currentWallpaper

    override suspend fun saveCurrentToGallery() {
        withContext(Dispatchers.IO) {
            WallpaperManager.saveCurrentToGallery(context)
        }
    }

    override suspend fun refreshWallpaper() {
        withContext(Dispatchers.IO) {
            WallpaperManager.refreshWallpaper(context)
        }
    }
}