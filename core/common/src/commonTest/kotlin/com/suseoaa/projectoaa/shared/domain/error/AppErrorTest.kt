package com.suseoaa.projectoaa.shared.domain.error

import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AppErrorTest {

    @Test
    fun `AppException 的 message 取自 userMessage 以兼容既有的展示代码`() {
        // UI 里大量代码是 `e.message ?: "默认文案"`，错误模型不能破坏这一点
        val e = AppException(AppError.Business(code = 40001, userMessage = "该账号已被禁用"))
        assertEquals("该账号已被禁用", e.message)
    }

    @Test
    fun `Http 错误默认文案带上状态码`() {
        assertEquals("请求失败: 502", AppError.Http(502).userMessage)
    }

    @Test
    fun `toAppError 对已经是 AppException 的不再重复包装`() {
        val original = AppError.SessionExpired
        val once = AppException(original)
        assertSame(original, once.toAppError())
        assertSame(once, once.asAppException())
    }

    @Test
    fun `未知异常归入 Unknown 并保留原始 message`() {
        val error = IllegalStateException("数据库连接已关闭").toAppError()
        assertIs<AppError.Unknown>(error)
        assertEquals("数据库连接已关闭", error.userMessage)
    }

    @Test
    fun `appRunCatching 捕获普通异常并包装成 AppException`() {
        val result = appRunCatching { error("炸了") }
        assertTrue(result.isFailure)
        assertIs<AppException>(result.exceptionOrNull())
        assertIs<AppError.Unknown>(result.appError)
    }

    @Test
    fun `appRunCatching 不吞掉协程取消`() {
        // runCatching 会把 CancellationException 当普通失败捕获，导致取消无法向上传播、
        // 结构化并发失效。appRunCatching 存在的主要理由就是这一条。
        assertFailsWith<CancellationException> {
            appRunCatching { throw CancellationException("被取消") }
        }
    }

    @Test
    fun `appFailure 产出的失败可以按类型分支`() {
        val result: Result<String> = appFailure(AppError.SessionExpired)
        assertEquals(AppError.SessionExpired, result.appError)
    }

    @Test
    fun `成功结果没有错误`() {
        assertNull(Result.success(1).appError)
    }

    @Test
    fun `非 AppException 的失败不冒充结构化错误`() {
        // 老代码里仍有直接 Result.failure(e) 的地方，这类失败不应被误读成某个分类
        assertNull(Result.failure<Int>(RuntimeException("裸异常")).appError)
    }
}
