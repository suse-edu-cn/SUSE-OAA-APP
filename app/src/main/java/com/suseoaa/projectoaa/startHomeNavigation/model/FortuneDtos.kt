package com.suseoaa.projectoaa.startHomeNavigation.model

import com.suseoaa.projectoaa.startHomeNavigation.viewmodel.DailyFortune
import com.suseoaa.projectoaa.startHomeNavigation.viewmodel.FortuneItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class DailyFortuneResponse(
    @SerialName("luck_level") val luckLevel: String,
    @SerialName("good_list") val goodList: List<FortuneItemDto>? = null,
    @SerialName("bad_list") val badList: List<FortuneItemDto>? = null
)

@Serializable
data class FortuneItemDto(
    @SerialName("title") val title: String,
    @SerialName("subtitle") val subtitle: String
)


fun DailyFortuneResponse.toDomainModel(): DailyFortune {
    return DailyFortune(
        luckLevel = this.luckLevel,
        goodList = this.goodList?.map { it.toDomainModel() } ?: emptyList(),
        badList = this.badList?.map { it.toDomainModel() } ?: emptyList()
    )
}

fun FortuneItemDto.toDomainModel(): FortuneItem {
    return FortuneItem(
        title = this.title,
        subtitle = this.subtitle
    )
}