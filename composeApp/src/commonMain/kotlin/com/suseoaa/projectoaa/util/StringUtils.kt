package com.suseoaa.projectoaa.util

import kotlin.math.pow
import kotlin.math.round

/**
 * 格式化字符串，例如将非官方字体、全角字符、特殊数学字母符号转为标准的ASCII字母和数字
 */
expect fun String.normalizeFont(): String

/**
 * KMP 兼容的数字格式化函数（四舍五入）
 */
fun Double.format(decimals: Int): String {
    if (this.isNaN() || this.isInfinite()) return this.toString()
    val factor = 10.0.pow(decimals)
    val rounded = round(this * factor) / factor
    val str = rounded.toString()
    val parts = str.split(".")
    return if (decimals <= 0) {
        parts[0]
    } else if (parts.size == 1) {
        "$str.${"0".repeat(decimals)}"
    } else {
        val intPart = parts[0]
        val decimalPart = parts[1]
        if (decimalPart.length >= decimals) {
            "$intPart.${decimalPart.take(decimals)}"
        } else {
            "$intPart.$decimalPart${"0".repeat(decimals - decimalPart.length)}"
        }
    }
}

fun Float.format(decimals: Int): String = this.toDouble().format(decimals)
