package com.suseoaa.projectoaa.shared.util

/**
 * 跨平台内容哈希工具
 * 用于调课通知去重。采用 FNV-1a 64-bit 算法。
 * 虽然不是密码学安全的 SHA-256，但对于短文本去重（如调课通知）速度快且足够唯一。
 */
object ContentHasher {
    /**
     * 计算字符串的 FNV-1a 64-bit 哈希，返回 16 字符的 hex 字符串
     */
    fun hashPrefix(input: String): String {
        var hash = -3750763034362895579L // FNV offset basis
        for (i in input.indices) {
            hash = hash xor input[i].code.toLong()
            hash *= 1099511628211L // FNV prime
        }
        return hash.toULong().toString(16).padStart(16, '0')
    }
}
