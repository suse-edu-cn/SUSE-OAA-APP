package com.suseoaa.projectoaa.navigation.repository

import com.suseoaa.projectoaa.common.network.LoliconApi
import javax.inject.Inject

class ImageRepositoryImpl @Inject constructor(): ImageRepository {

    private val loliconApi = LoliconApi.create()

    override suspend fun fetchPlaceholderImage(): ImageResult {
        return try {
            val response = loliconApi.getSetu(
                r18 = 0,
                excludeAI = true,
                num = 1,
                aspectRatio = "2",
                size = "regular"
            )
            if (response.error.isNullOrEmpty() && !response.data.isNullOrEmpty()) {
                val url = response.data[0].urls["regular"]
                    ?: response.data[0].urls["original"]

                if (url != null) {
                    ImageResult.Success(url)
                } else {
                    ImageResult.Error(Exception("URL is null in response"))
                }
            } else {
                ImageResult.Error(Exception(response.error ?: "Empty data"))
            }
        } catch (e: Exception) {
            ImageResult.Error(e)
        }
    }
}