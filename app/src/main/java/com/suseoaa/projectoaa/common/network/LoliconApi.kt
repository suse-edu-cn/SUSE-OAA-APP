package com.suseoaa.projectoaa.common.network

import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import com.squareup.moshi.Moshi
import com.squareup.moshi.JsonClass
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.converter.moshi.MoshiConverterFactory

@JsonClass(generateAdapter = true)
data class LoliconResponse(
    val data: List<LoliconData>?,
    val error: String? = null
)

@JsonClass(generateAdapter = true)
data class LoliconData(
    val pid: Long,
    val title: String?,
    val author: String?,
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
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Safari/537.36")
                        .build()
                    chain.proceed(request)
                }
                .build()

            return Retrofit.Builder()
                .baseUrl("https://api.lolicon.app/")
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(LoliconApi::class.java)
        }
    }
}