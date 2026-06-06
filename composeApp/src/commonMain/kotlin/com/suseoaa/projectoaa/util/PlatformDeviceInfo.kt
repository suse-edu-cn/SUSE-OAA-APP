package com.suseoaa.projectoaa.util

/**
 * 跨平台设备硬件信息接口
 * Android 实现读取真实系统 API；iOS 实现通过 UIDevice/sysctl 获取
 */
data class DeviceInfo(
    /** 总物理内存（字节） */
    val totalRam: Long,
    /** 当前可用内存（字节） */
    val availableRam: Long,
    /** CPU 型号或描述，例如 "Snapdragon 8 Gen 3" */
    val cpuModel: String,
    /** GPU 渲染器字符串，例如 "Adreno (TM) 750" */
    val gpuRenderer: String,
    /** 是否检测到专用 NPU 支持 */
    val hasNpu: Boolean,
    /** NPU 描述，例如 "Hexagon NPU" 或 "Apple Neural Engine" */
    val npuDescription: String,
    /** 总存储空间（字节） */
    val totalStorage: Long,
    /** 当前可用存储空间（字节） */
    val availableStorage: Long,
    /** 操作系统版本字符串，例如 "Android 14 (API 34)" */
    val osVersion: String,
    /** SoC 制造商，例如 "Qualcomm", "MediaTek", "Apple" */
    val socVendor: String
)

/** 推荐模型等级 */
enum class ModelRecommendationLevel {
    /** 推荐 Gemma 4 E4B（12GB+ RAM） */
    E4B_RECOMMENDED,
    /** 推荐 Gemma 4 E2B（6~12GB RAM） */
    E2B_RECOMMENDED,
    /** 设备不满足最低要求（< 6GB RAM） */
    NOT_RECOMMENDED
}

data class ModelRecommendation(
    val level: ModelRecommendationLevel,
    val modelName: String,
    val modelSizeDesc: String,
    val downloadUrlHuggingFace: String,
    val reason: String
)

data class AiModelMetadata(
    val id: String,
    val name: String,
    val sizeDesc: String,
    val downloadUrl: String,
    val recommendedLevel: ModelRecommendationLevel
)

val AvailableAiModels = listOf(
    AiModelMetadata(
        id = "gemma-4-e2b-int4",
        name = "Gemma 2B (INT4 极致性能)",
        sizeDesc = "约 1.25GB",
        downloadUrl = "https://huggingface.co/autoocrat0413/gemma-2b-it-gpu-int4-mediapipe/resolve/main/gemma-2b-it-cpu-int4.bin",
        recommendedLevel = ModelRecommendationLevel.E2B_RECOMMENDED
    ),
    AiModelMetadata(
        id = "gemma-4-e2b-int8",
        name = "Gemma 2B (INT8 高精度)",
        sizeDesc = "约 2.76GB",
        downloadUrl = "https://huggingface.co/CarlosJefte/Gemma-2-2b-mediapipe/resolve/main/gemma2-2b-it-gpu-int8.bin",
        recommendedLevel = ModelRecommendationLevel.E2B_RECOMMENDED
    ),
    AiModelMetadata(
        id = "gemma-4-e4b-int4",
        name = "Gemma 2 2B (INT4 极致性能)",
        sizeDesc = "约 1.25GB",
        downloadUrl = "https://huggingface.co/autoocrat0413/gemma-2b-it-gpu-int4-mediapipe/resolve/main/gemma-2b-it-cpu-int4.bin",
        recommendedLevel = ModelRecommendationLevel.E4B_RECOMMENDED
    ),
    AiModelMetadata(
        id = "gemma-4-e4b-int8",
        name = "Gemma 2 2B (INT8 高精度)",
        sizeDesc = "约 2.76GB",
        downloadUrl = "https://huggingface.co/CarlosJefte/Gemma-2-2b-mediapipe/resolve/main/gemma2-2b-it-cpu-int8.task",
        recommendedLevel = ModelRecommendationLevel.E4B_RECOMMENDED
    )
)

/**
 * 跨平台设备信息查询接口（expect/actual）
 * 注意：调用 queryDeviceInfo() 可能会短暂触发 OpenGL 上下文，建议在后台线程调用。
 */
expect object PlatformDeviceInfo {
    /**
     * 同步查询设备硬件信息
     * iOS 端使用 sysctl 系列 API；Android 端使用 ActivityManager + StatFs + OpenGL。
     */
    fun queryDeviceInfo(): DeviceInfo
}

/** 根据设备信息计算推荐模型 */
fun computeModelRecommendation(info: DeviceInfo): ModelRecommendation {
    val totalRamGb = info.totalRam / (1024f * 1024f * 1024f)
    return when {
        totalRamGb >= 16f -> ModelRecommendation(
            level = ModelRecommendationLevel.E4B_RECOMMENDED,
            modelName = AvailableAiModels.find { it.id == "gemma-4-e4b-int8" }?.name ?: "",
            modelSizeDesc = AvailableAiModels.find { it.id == "gemma-4-e4b-int8" }?.sizeDesc ?: "",
            downloadUrlHuggingFace = AvailableAiModels.find { it.id == "gemma-4-e4b-int8" }?.downloadUrl ?: "",
            reason = "您的设备拥有超大内存（${totalRamGb.toInt()}GB RAM），推荐体验极致高精度的 Gemma 4 E4B INT8 模型！"
        )
        totalRamGb >= 12f -> ModelRecommendation(
            level = ModelRecommendationLevel.E4B_RECOMMENDED,
            modelName = AvailableAiModels.find { it.id == "gemma-4-e4b-int4" }?.name ?: "",
            modelSizeDesc = AvailableAiModels.find { it.id == "gemma-4-e4b-int4" }?.sizeDesc ?: "",
            downloadUrlHuggingFace = AvailableAiModels.find { it.id == "gemma-4-e4b-int4" }?.downloadUrl ?: "",
            reason = "设备运存较大（${totalRamGb.toInt()}GB RAM），完全可以驾驭强大的 Gemma 4 E4B INT4！"
        )
        totalRamGb >= 8f -> ModelRecommendation(
            level = ModelRecommendationLevel.E2B_RECOMMENDED,
            modelName = AvailableAiModels.find { it.id == "gemma-4-e2b-int8" }?.name ?: "",
            modelSizeDesc = AvailableAiModels.find { it.id == "gemma-4-e2b-int8" }?.sizeDesc ?: "",
            downloadUrlHuggingFace = AvailableAiModels.find { it.id == "gemma-4-e2b-int8" }?.downloadUrl ?: "",
            reason = "设备运存不错（${totalRamGb.toInt()}GB RAM），推荐体验高精度的 Gemma 4 E2B INT8，兼顾性能与画质。"
        )
        totalRamGb >= 6f -> ModelRecommendation(
            level = ModelRecommendationLevel.E2B_RECOMMENDED,
            modelName = AvailableAiModels.find { it.id == "gemma-4-e2b-int4" }?.name ?: "",
            modelSizeDesc = AvailableAiModels.find { it.id == "gemma-4-e2b-int4" }?.sizeDesc ?: "",
            downloadUrlHuggingFace = AvailableAiModels.find { it.id == "gemma-4-e2b-int4" }?.downloadUrl ?: "",
            reason = "对于 ${totalRamGb.toInt()}GB 运存的设备，Gemma 4 E2B INT4 是最流畅的选择！"
        )
        else -> ModelRecommendation(
            level = ModelRecommendationLevel.NOT_RECOMMENDED,
            modelName = "不支持本地大模型",
            modelSizeDesc = "设备内存不足",
            downloadUrlHuggingFace = "",
            reason = "抱歉，端侧大模型运行时需要至少占用 3GB 独立显存/内存，您的设备（${totalRamGb.toInt()}GB RAM）可能面临随时闪退的风险，不建议下载。"
        )
    }
}

/** 格式化字节为可读字符串（GB / MB） */
fun Long.toReadableStorage(): String {
    val gb = this / (1024.0 * 1024.0 * 1024.0)
    return if (gb >= 1.0) "%.1f GB".format(gb) else "%.0f MB".format(this / (1024.0 * 1024.0))
}
