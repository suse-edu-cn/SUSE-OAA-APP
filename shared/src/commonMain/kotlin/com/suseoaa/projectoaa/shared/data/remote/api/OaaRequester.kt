package com.suseoaa.projectoaa.shared.data.remote.api

import com.suseoaa.projectoaa.shared.data.remote.ApiConfig
import com.suseoaa.projectoaa.shared.domain.error.AppError
import com.suseoaa.projectoaa.shared.domain.error.appFailure
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.decodeURLPart
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement

/** 二进制下载结果。 */
data class BinaryPayload(
    val bytes: ByteArray,
    val fileName: String,
    val contentType: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BinaryPayload) return false
        return fileName == other.fileName &&
            contentType == other.contentType &&
            bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + contentType.hashCode()
        return result
    }
}

/**
 * OAA v2 接口的统一调用入口。
 *
 * 后端响应统一是 `{code, message, data}`，但细节并不整齐：上传接口的 code 是字符串
 * "200"，400 / 403 时 HTTP 状态码和 body 里的 code 都会变，个别接口文档里还没有外壳。
 * 这些差异都在这里抹平，Repository 拿到的只有 `Result<T>`：成功是 data，失败是带
 * 可直接展示文案的 [AppError]。
 */
class OaaRequester(
    @PublishedApi internal val client: HttpClient,
    @PublishedApi internal val json: Json
) {

    suspend inline fun <reified T> get(
        path: String,
        fallbackMessage: String,
        params: Map<String, Any?> = emptyMap()
    ): Result<T> = execute(fallbackMessage, { decode<T>(it) }) {
        client.get(ApiConfig.OAA_BASE + path) {
            params.forEach { (key, value) -> if (value != null) parameter(key, value) }
        }
    }

    suspend inline fun <reified B, reified T> post(
        path: String,
        body: B,
        fallbackMessage: String
    ): Result<T> = execute(fallbackMessage, { decode<T>(it) }) {
        client.post(ApiConfig.OAA_BASE + path) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    /** data 为 Unit 时忽略内容；为可空类型时 null 原样返回。 */
    @PublishedApi
    internal inline fun <reified T> decode(element: JsonElement?): T {
        if (T::class == Unit::class) return Unit as T
        return json.decodeFromJsonElement<T>(element ?: JsonNull)
    }

    suspend fun <T> execute(
        fallbackMessage: String,
        decode: (JsonElement?) -> T,
        send: suspend () -> HttpResponse
    ): Result<T> {
        return try {
            val response = send()
            val text = response.bodyAsText()
            val element = if (text.isBlank()) null else runCatching { json.parseToJsonElement(text) }.getOrNull()
            val envelope = element as? JsonObject

            if (envelope != null && "code" in envelope) {
                val code = (envelope["code"] as? JsonPrimitive)?.contentOrNull?.toIntOrNull()
                val message = (envelope["message"] as? JsonPrimitive)?.contentOrNull.orEmpty()
                if (code == 200 && response.status.isSuccess()) {
                    val data = envelope["data"]?.takeUnless { it is JsonNull }
                    Result.success(decode(data))
                } else {
                    appFailure(businessError(code, response.status, message, fallbackMessage))
                }
            } else if (!response.status.isSuccess()) {
                appFailure(httpError(response.status, fallbackMessage))
            } else {
                // 2xx 却没有外壳：/application/me 在文档里就是直接返回对象
                Result.success(decode(element))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: SerializationException) {
            appFailure(AppError.Parse("$fallbackMessage：数据格式与预期不符"))
        } catch (e: IllegalArgumentException) {
            appFailure(AppError.Parse("$fallbackMessage：数据格式与预期不符"))
        } catch (e: Exception) {
            appFailure(AppError.Network(e))
        }
    }

    private fun businessError(
        code: Int?,
        status: HttpStatusCode,
        message: String,
        fallbackMessage: String
    ): AppError {
        if (message.isNotBlank()) return AppError.Business(code = code, userMessage = message)
        return when (code ?: status.value) {
            401 -> AppError.SessionExpired
            403 -> AppError.Business(code = 403, userMessage = "当前账号无权限执行此操作")
            else -> AppError.Business(code = code, userMessage = fallbackMessage)
        }
    }

    private fun httpError(status: HttpStatusCode, fallbackMessage: String): AppError = when (status.value) {
        401 -> AppError.SessionExpired
        403 -> AppError.Http(status.value, "当前账号无权限执行此操作")
        else -> AppError.Http(status.value, "$fallbackMessage（HTTP ${status.value}）")
    }

    internal fun errorFromResponse(
        status: HttpStatusCode,
        bodyText: String,
        fallbackMessage: String
    ): AppError {
        val element = if (bodyText.isBlank()) null else runCatching { json.parseToJsonElement(bodyText) }.getOrNull()
        val envelope = element as? JsonObject
        if (envelope != null && "code" in envelope) {
            val code = (envelope["code"] as? JsonPrimitive)?.contentOrNull?.toIntOrNull()
            val message = (envelope["message"] as? JsonPrimitive)?.contentOrNull.orEmpty()
            return businessError(code, status, message, fallbackMessage)
        }
        return httpError(status, fallbackMessage)
    }

    suspend fun getBinary(
        path: String,
        fallbackMessage: String,
        params: Map<String, Any?> = emptyMap(),
        defaultFileName: String = "export.xlsx"
    ): Result<BinaryPayload> {
        return try {
            val response = client.get(ApiConfig.OAA_BASE + path) {
                params.forEach { (key, value) -> if (value != null) parameter(key, value) }
            }
            val responseContentType = response.contentType()
            val isJson = responseContentType?.match(ContentType.Application.Json) == true
            if (!response.status.isSuccess() || isJson) {
                val text = response.bodyAsText()
                val element = if (text.isBlank()) null else runCatching { json.parseToJsonElement(text) }.getOrNull()
                val envelope = element as? JsonObject
                if (envelope != null && "code" in envelope) {
                    val code = (envelope["code"] as? JsonPrimitive)?.contentOrNull?.toIntOrNull()
                    val message = (envelope["message"] as? JsonPrimitive)?.contentOrNull.orEmpty()
                    return appFailure(businessError(code, response.status, message, fallbackMessage))
                }
                return appFailure(httpError(response.status, fallbackMessage))
            }
            val bytes = response.readRawBytes()
            val fileName = parseContentDispositionFileName(response.headers[HttpHeaders.ContentDisposition])
                ?.ifBlank { null } ?: defaultFileName
            val mimeType = responseContentType?.withoutParameters()?.toString().orEmpty()
                .ifBlank { DEFAULT_XLSX_MIME }
            Result.success(BinaryPayload(bytes = bytes, fileName = fileName, contentType = mimeType))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            appFailure(AppError.Network(e))
        }
    }

    private fun parseContentDispositionFileName(header: String?): String? {
        if (header.isNullOrBlank()) return null
        val extMatch = FILENAME_STAR_REGEX.find(header)
        if (extMatch != null) {
            val raw = extMatch.groupValues[1].trim().trim('"')
            val decoded = runCatching { raw.decodeURLPart() }.getOrDefault(raw)
                .substringAfterLast('/').substringAfterLast('\\').trim()
            if (decoded.isNotEmpty()) return decoded
        }
        val plainMatch = FILENAME_REGEX.find(header)
        if (plainMatch != null) {
            val raw = (plainMatch.groupValues[1].ifEmpty { plainMatch.groupValues[2] }).trim().trim('"')
            val decoded = if ('%' in raw) {
                runCatching { raw.decodeURLPart() }.getOrDefault(raw)
            } else {
                raw
            }.substringAfterLast('/').substringAfterLast('\\').trim()
            if (decoded.isNotEmpty()) return decoded
        }
        return null
    }

    private companion object {
        const val DEFAULT_XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        val FILENAME_STAR_REGEX = Regex("""filename\*\s*=\s*(?:[A-Za-z0-9_-]+''|\S*'')?([^;\r\n]+)""", RegexOption.IGNORE_CASE)
        val FILENAME_REGEX = Regex("""filename\s*=\s*"([^"]+)"|filename\s*=\s*([^;\s]+)""", RegexOption.IGNORE_CASE)
    }
}
