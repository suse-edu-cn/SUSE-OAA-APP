package com.suseoaa.projectoaa.shared.data.repository

/**
 * API 调用结果封装
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val code: Int? = null, val exception: Throwable? = null) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}

/**
 * 扩展函数：map 转换
 */
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> {
    return when (this) {
        is Result.Success -> Result.Success(transform(data))
        is Result.Error -> this
        is Result.Loading -> this
    }
}

/**
 * 扩展函数：onSuccess 处理成功
 */
inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) {
        action(data)
    }
    return this
}

/**
 * 扩展函数：onError 处理错误
 */
inline fun <T> Result<T>.onError(action: (String, Int?, Throwable?) -> Unit): Result<T> {
    if (this is Result.Error) {
        action(message, code, exception)
    }
    return this
}

/**
 * 扩展函数：getOrNull
 */
fun <T> Result<T>.getOrNull(): T? {
    return when (this) {
        is Result.Success -> data
        else -> null
    }
}

/**
 * 扩展函数：getOrDefault
 */
fun <T> Result<T>.getOrDefault(defaultValue: T): T {
    return when (this) {
        is Result.Success -> data
        else -> defaultValue
    }
}
