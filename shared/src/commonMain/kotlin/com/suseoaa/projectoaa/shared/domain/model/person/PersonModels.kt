package com.suseoaa.projectoaa.shared.domain.model.person

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

/**
 * 对象存储里的文件：[uri] 是存储路径（提交给后端用），[url] 是带签名的临时下载地址（展示用）。
 *
 * 兼容后端返回 `{uri, url}` 对象、单字符串路径/链接或 `null`。
 * 只有相对路径可写入 [uri]；旧接口仅返回 URL 时只能展示，修改照片前需重新上传。
 */
@Serializable(with = StoredFileSerializer::class)
data class StoredFile(
    @SerialName("uri") val uri: String = "",
    @SerialName("url") val url: String = ""
)

internal object StoredFileSerializer : KSerializer<StoredFile> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("StoredFile") {
        element<String>("uri", isOptional = true)
        element<String>("url", isOptional = true)
    }

    override fun deserialize(decoder: Decoder): StoredFile {
        val jsonDecoder = decoder as? JsonDecoder ?: return StoredFile()
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonObject -> {
                val rawUri = (element["uri"] as? JsonPrimitive)?.contentOrNull.orEmpty().trim()
                val rawUrl = (element["url"] as? JsonPrimitive)?.contentOrNull.orEmpty().trim()
                StoredFile(
                    uri = OssObjectKey.parse(rawUri).orEmpty(),
                    url = rawUrl.ifBlank { rawUri.takeIf(::isHttpUrl).orEmpty() }
                )
            }
            is JsonPrimitive -> {
                val raw = element.contentOrNull?.trim().orEmpty()
                if (raw.isEmpty()) {
                    StoredFile()
                } else if (isHttpUrl(raw)) {
                    StoredFile(url = raw)
                } else {
                    StoredFile(uri = OssObjectKey.parse(raw).orEmpty())
                }
            }
            is JsonNull -> StoredFile()
            else -> StoredFile()
        }
    }

    private fun isHttpUrl(value: String): Boolean =
        value.startsWith("http://", ignoreCase = true) || value.startsWith("https://", ignoreCase = true)

    override fun serialize(encoder: Encoder, value: StoredFile) {
        val jsonEncoder = encoder as? JsonEncoder
        if (jsonEncoder != null) {
            jsonEncoder.encodeJsonElement(
                buildJsonObject {
                    put("uri", value.uri)
                    put("url", value.url)
                }
            )
        } else {
            val composite = encoder.beginStructure(descriptor)
            composite.encodeStringElement(descriptor, 0, value.uri)
            composite.encodeStringElement(descriptor, 1, value.url)
            composite.endStructure(descriptor)
        }
    }
}

/** GET /user/me */
@Serializable
data class PersonData(
    @SerialName("user_id") val userId: Int = 0,
    @SerialName("student_id") val studentId: String = "",
    @SerialName("username") val username: String = "",
    @SerialName("avatar") val avatar: StoredFile = StoredFile(),
    @SerialName("name") val name: String = "",
    @SerialName("email") val email: String = "",
    @SerialName("department") val department: String = "",
    @SerialName("role") val role: String = "",
    /** 年级（四位年份整数，如 2024）；未设置或老版本后端未返回时为 null / 0 */
    @SerialName("grade") val grade: Int? = null,
    /** null 表示旧后端未返回，不能据此推断毕业状态。 */
    @SerialName("is_graduated") val isGraduated: Boolean? = null,
    // 文档没写，但线上接口返回了职位等级；老版本后端没有时为 null
    @SerialName("role_level") val roleLevel: Int? = null,
    @SerialName("is_interviewer") val isInterviewer: Boolean? = null,
    /** 是否正处于注销冷静期；为 false 时后端不返回 [scheduledDeleteAt] */
    @SerialName("is_deleting") val isDeleting: Boolean = false,
    /** 预计永久删除时间（如 "2026-10-26 15:04:05"），仅 [isDeleting] 为 true 时返回 */
    @SerialName("scheduled_delete_at") val scheduledDeleteAt: String? = null
) {
    val avatarUrl: String get() = avatar.url
    val resolvedGrade: Int? get() = grade?.takeIf(EnrollmentGrade::isValid)
}

/**
 * 当前登录用户及其权限等级。
 *
 * 等级优先取 /user/me 的 role_level，没有时再拿 /role/list 按名称对出来；都对不上按 0 处理。
 * 界面上只隐藏入口，真正的权限仍以后端校验为准。
 */
data class CurrentUser(
    val person: PersonData,
    val level: Int,
    val departmentId: Int?,
    val roleId: Int?
)

/** POST /user/me/update，username / email / avatar 必填，不改的也要原样带上；grade 为四位年份年级。 */
@Serializable
data class UpdateMeRequest(
    @SerialName("username") val username: String,
    @SerialName("email") val email: String,
    @SerialName("avatar") val avatar: String,
    @SerialName("grade") val grade: Int? = null
)

/** 基础设施探针检测结果（GET /livez 与 GET /healthz）。 */
data class ServerHealthStatus(
    val live: Boolean,
    val ready: Boolean,
    val latencyMs: Long,
    val message: String
) {
    val isHealthy: Boolean get() = live && ready
}

