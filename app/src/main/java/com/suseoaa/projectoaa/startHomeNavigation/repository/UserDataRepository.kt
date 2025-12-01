package com.suseoaa.projectoaa.navigation.repository

import kotlinx.coroutines.flow.Flow

interface UserDataRepository {
    // 签到相关
    fun getLastCheckInDate(): String?
    fun saveCheckInDate(date: String)
    fun getCheckInCount(): Int
    fun saveCheckInCount(count: Int)
    fun getCachedImage(): Pair<String?, String?>
    fun saveCachedImage(date: String, url: String)
}