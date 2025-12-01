package com.suseoaa.projectoaa.common.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class LoliconResponse(
    val data: List<LoliconData>? = null,
    val error: String? = null
)

@Serializable
data class LoliconData(
    val pid: Long,
    val title: String? = null,
    val author: String? = null,
    val urls: Map<String, String>
)

interface LoliconApi {
    /**
     * 注意：这个接口是给 WallpaperManager 专用的，
     * 它不应该通过 NetworkModule.kt 创建，因为它有自己的 BaseUrl 和 Client。
     */
    @GET("setu/v2")
    suspend fun getSetu(
        @Query("r18") r18: Int = 0,
        @Query("excludeAI") excludeAI: Boolean = true,
        @Query("num") num: Int = 1,
        @Query("size") size: String = "regular",
        @Query("tag") tag: String? = "萝莉",
        @Query("aspectRatio") aspectRatio: String? = null
    ): LoliconResponse

    companion object {
        fun create(): LoliconApi {
            val json = Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
                isLenient = true
            }

            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Safari/537.36")
                        .build()
                    chain.proceed(request)
                }
                .build()

            val contentType = "application/json".toMediaType()

            return Retrofit.Builder()
                .baseUrl("https://api.lolicon.app/")
                .client(client)
                .addConverterFactory(json.asConverterFactory(contentType))
                .build()
                .create(LoliconApi::class.java)
        }
    }
}