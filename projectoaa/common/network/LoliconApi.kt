package com.suseoaa.projectoaa.common.network

import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import com.squareup.moshi.Moshi
import com.squareup.moshi.JsonClass
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory

// 1. 修改：增加 error 字段，且让 data 可空，防止解析崩溃
@JsonClass(generateAdapter = true)
data class LoliconResponse(
    val data: List<LoliconData>?, // 改为可空
    val error: String? = null     // 增加错误信息字段
)

@JsonClass(generateAdapter = true)
data class LoliconData(
    val pid: Long,
    val title: String,
    val author: String,
    val urls: Map<String, String>
)

interface LoliconApi {
    @GET("setu/v2")
    suspend fun getSetu(
        @Query("r18") r18: Int = 0,
        @Query("excludeAI") excludeAI: Boolean = true,
        @Query("num") num: Int = 1,
        @Query("size") size: String = "regular",
        @Query("tag") tag: String? = null,
        @Query("aspectRatio") aspectRatio: String? = null
    ): LoliconResponse

    companion object {
        fun create(): LoliconApi {
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl("https://api.lolicon.app/")
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(LoliconApi::class.java)
        }
    }
}