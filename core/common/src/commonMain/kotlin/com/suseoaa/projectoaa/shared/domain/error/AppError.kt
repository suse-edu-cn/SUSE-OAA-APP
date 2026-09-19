package com.suseoaa.projectoaa.shared.domain.error

import kotlin.coroutines.cancellation.CancellationException

/**
 * 全应用统一的失败分类。
 *
 * 改造前失败一律是 `Exception("请求失败: 500")` 这样的裸异常，调用方只能靠字符串
 * 判断出了什么事；会话过期甚至各自定义了两份 `SessionExpiredException` 嵌套类，
 * 互相 catch 不到。这里把失败收敛成有限几种，调用方可以按类型分支。
 *
 * [userMessage] 是可以直接显示给用户的文案。[AppException] 会把它作为
 * `Exception.message`，所以既有的 `e.message` 展示代码行为不变。
 */
sealed interface AppError {

    val userMessage: String

    /** 传输层故障：连不上、超时、DNS 失败等。 */
    data class Network(val cause: Throwable? = null) : AppError {
        override val userMessage: String = "网络连接失败，请检查网络后重试"
    }

    /** 教务系统会话失效，需要重新登录后重试。 */
    data object SessionExpired : AppError {
        override val userMessage: String = "登录已过期，请重新登录"
    }

    /** 凭据不正确。 */
    data class Unauthorized(override val userMessage: String = "账号或密码不正确") : AppError

    /** 服务端返回了非 2xx 状态码。 */
    data class Http(val status: Int, override val userMessage: String = "请求失败: $status") : AppError

    /** 响应拿到了，但解析不出来——通常意味着对方接口变了。 */
    data class Parse(override val userMessage: String = "数据解析失败") : AppError

    /** 请求成功但业务上被拒绝，[code] 为服务端业务码（没有则为 null）。 */
    data class Business(val code: Int? = null, override val userMessage: String) : AppError

    /** 兜底。能归到上面任何一类就不要用这一类。 */
    data class Unknown(val cause: Throwable? = null, override val userMessage: String = "发生未知错误") : AppError
}

/**
 * 承载 [AppError] 的异常。
 *
 * 仍然继承 Exception，是为了能塞进 [Result.failure]，与既有的 `Result<T>` 签名兼容；
 * `message` 直接取 [AppError.userMessage]，因此 UI 里所有 `e.message` 的展示原样可用。
 */
class AppException(
    val error: AppError,
    cause: Throwable? = null,
) : Exception(error.userMessage, cause)

/** 把任意异常归类。已经是 [AppException] 的原样返回。 */
fun Throwable.toAppError(): AppError = when (this) {
    is AppException -> error
    else -> AppError.Unknown(this, message ?: "发生未知错误")
}

/** 统一包装成 [AppException]，避免重复包裹。 */
fun Throwable.asAppException(): AppException =
    this as? AppException ?: AppException(toAppError(), this)

/** 构造一个失败结果。 */
fun <T> appFailure(error: AppError): Result<T> = Result.failure(AppException(error))

/**
 * 取代 [runCatching]。
 *
 * 区别在于**不吞掉 [CancellationException]**：`runCatching` 会把协程取消当成普通失败
 * 捕获，导致取消无法向上传播、结构化并发失效——这是 Kotlin 协程里最常见的一类隐蔽 bug。
 */
inline fun <T> appRunCatching(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e.asAppException())
    }

/** 失败时取出结构化错误，成功或非 AppException 失败时返回 null。 */
val <T> Result<T>.appError: AppError?
    get() = exceptionOrNull()?.let { it as? AppException }?.error
