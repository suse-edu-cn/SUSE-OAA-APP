package com.suseoaa.projectoaa.ui.screen.person

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 主题色输入框接受 #RRGGBB / rgb(...) / hsl(...) 三种写法，还要能拒绝乱输入。
 * 解析错了轻则配色不对，重则用户的主题被改成非预期的颜色，属于典型易错的纯逻辑。
 */
class ColorParsingTest {

    private fun assertClose(expected: Float, actual: Float, tolerance: Float = 0.01f) =
        assertTrue(abs(expected - actual) <= tolerance, "期望 ≈$expected，实际 $actual")

    // ---------- 十六进制 ----------

    @Test
    fun `解析六位十六进制`() {
        val c = assertNotNull("#FF8800".toColorOrNull())
        assertClose(1f, c.red); assertClose(0.533f, c.green); assertClose(0f, c.blue)
        assertClose(1f, c.alpha)
    }

    @Test
    fun `十六进制大小写与前缀都能接受`() {
        assertEquals("#FF8800".toColorOrNull(), "#ff8800".toColorOrNull())
    }

    @Test
    fun `颜色能往返转成十六进制文本`() {
        val hex = Color(0xFF3366CC).toHexString()
        assertEquals(Color(0xFF3366CC), assertNotNull(hex.toColorOrNull()))
    }

    @Test
    fun `非法十六进制返回 null 而不是抛异常`() {
        assertNull("#GGGGGG".toColorOrNull())
        assertNull("".toColorOrNull())
        assertNull(null.toColorOrNull())
    }

    // ---------- rgb() ----------

    @Test
    fun `解析 rgb 三通道`() {
        val c = assertNotNull(parseRgbColor("rgb(255, 136, 0)"))
        assertClose(1f, c.red); assertClose(0.533f, c.green); assertClose(0f, c.blue)
    }

    @Test
    fun `解析 rgba 带透明度`() {
        val c = assertNotNull(parseRgbColor("rgba(0, 0, 0, 0.5)"))
        assertClose(0.5f, c.alpha)
    }

    @Test
    fun `rgb 通道支持百分比写法`() {
        val c = assertNotNull(parseRgbColor("rgb(100%, 0%, 0%)"))
        assertClose(1f, c.red); assertClose(0f, c.green)
    }

    @Test
    fun `rgb 通道超出范围时被夹到边界而不是溢出`() {
        val c = assertNotNull(parseRgbColor("rgb(999, -20, 0)"))
        assertClose(1f, c.red)
        assertClose(0f, c.green)
    }

    @Test
    fun `参数个数不对的 rgb 返回 null`() {
        assertNull(parseRgbColor("rgb(1, 2)"))
        assertNull(parseRgbColor("rgb(1, 2, 3, 4, 5)"))
        assertNull(parseRgbColor("rgb(a, b, c)"))
    }

    // ---------- hsl() ----------

    @Test
    fun `hsl 红色`() {
        val c = assertNotNull(parseHslColor("hsl(0, 100%, 50%)"))
        assertClose(1f, c.red); assertClose(0f, c.green); assertClose(0f, c.blue)
    }

    @Test
    fun `hsl 支持 deg 后缀与 alpha`() {
        val c = assertNotNull(parseHslColor("hsla(120deg, 100%, 50%, 0.25)"))
        assertClose(1f, c.green)
        assertClose(0.25f, c.alpha)
    }

    @Test
    fun `饱和度为零时得到灰阶`() {
        val c = assertNotNull(parseHslColor("hsl(210, 0%, 50%)"))
        assertClose(c.red, c.green); assertClose(c.green, c.blue)
    }

    // ---------- 统一入口 ----------

    @Test
    fun `统一入口按十六进制到 rgb 到 hsl 的顺序尝试`() {
        assertNotNull(parseColorInput("  #112233  "))
        assertNotNull(parseColorInput("rgb(1,2,3)"))
        assertNotNull(parseColorInput("hsl(1,2%,3%)"))
    }

    @Test
    fun `空白与无法识别的输入返回 null`() {
        assertNull(parseColorInput("   "))
        assertNull(parseColorInput("蓝色"))
        assertNull(parseColorInput("rgb"))
    }

    // ---------- 辅助解析 ----------

    @Test
    fun `alpha 大于 1 时按 0 到 255 解释`() {
        assertClose(1f, assertNotNull(parseAlpha("255")))
        assertClose(0.5f, assertNotNull(parseAlpha("128")), tolerance = 0.01f)
        assertClose(0.3f, assertNotNull(parseAlpha("0.3")))
        assertClose(0.4f, assertNotNull(parseAlpha("40%")))
    }

    @Test
    fun `百分比会被夹在 0 到 1 之间`() {
        assertClose(1f, assertNotNull(parsePercent("300%")))
        assertClose(0f, assertNotNull(parsePercent("-50%")))
        assertNull(parsePercent("abc"))
    }

    // ---------- HSV 转换 ----------

    @Test
    fun `颜色转 HSV 后色相与饱和度合理`() {
        val hsv = Color(0xFFFF0000).toHsvColor()
        assertClose(0f, hsv.h, tolerance = 1f)
        assertClose(1f, hsv.s)
        assertClose(1f, hsv.v)
    }

    @Test
    fun `黑色的饱和度与明度都是零`() {
        val hsv = Color(0xFF000000).toHsvColor()
        assertClose(0f, hsv.s)
        assertClose(0f, hsv.v)
    }
}
