package com.suseoaa.projectoaa.navigation.repository

import javax.inject.Inject

class FeedbackRepositoryImpl @Inject constructor() : FeedbackRepository {
    override suspend fun submit(text: String): Boolean {
        kotlinx.coroutines.delay(1500)

        return true // 模拟成功
    }
}