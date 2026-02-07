package com.suseoaa.projectoaa.util

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.posix.memcpy

/**
 * 打卡系统专用 RSA 加密器 - iOS 实现
 * 模拟 Python Auto_SUSE_By_Password.py 的 rsa_encrypt_custom 函数
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual object CheckinRSAEncryptor {

    /**
     * 使用原始 RSA 幂模运算加密
     * @param plainText 明文（已反转的密码）
     * @param modulusHex 模数（十六进制字符串）
     * @param exponentHex 指数（十六进制字符串）
     * @return 加密后的十六进制字符串
     */
    actual fun encrypt(plainText: String, modulusHex: String, exponentHex: String): String {
        try {
            // 将明文转换为字节数组，然后转为大整数
            // 模拟 Python: message_int = 0; for byte in message_bytes: message_int = (message_int << 8) + byte
            val messageBytes = plainText.encodeToByteArray()
            val messageInt = CheckinBigInt.fromAsciiBytes(messageBytes)

            // 将十六进制的 modulus 和 exponent 转为 BigInteger
            val modulusInt = CheckinBigInt.fromHexString(modulusHex)
            val exponentInt = CheckinBigInt.fromHexString(exponentHex)

            // 执行 RSA 加密: encrypted = message^exponent mod modulus
            val encryptedInt = messageInt.modPow(exponentInt, modulusInt)

            // 转为十六进制字符串，补零到 256 位（1024位密钥）
            val encryptedHex = encryptedInt.toHexString()
            return encryptedHex.lowercase().padStart(256, '0')
        } catch (e: Exception) {
            println("[CheckinRSA] Encryption error: ${e.message}")
            throw RuntimeException("RSA加密失败: ${e.message}")
        }
    }
}

/**
 * 简单的大整数实现，用于打卡系统 RSA 运算
 * 内部使用 UInt 数组存储，小端序
 */
private class CheckinBigInt private constructor(
    private val digits: UIntArray,
    private val negative: Boolean = false
) {
    companion object {
        private const val BASE_BITS = 32
        private const val BASE = 0x100000000UL  // 2^32

        val ZERO = CheckinBigInt(uintArrayOf(0u), false)
        val ONE = CheckinBigInt(uintArrayOf(1u), false)

        /**
         * 从十六进制字符串创建 BigInt
         */
        fun fromHexString(hex: String): CheckinBigInt {
            if (hex.isEmpty() || hex == "0") return ZERO

            val cleanHex = hex.removePrefix("0x").removePrefix("0X")
            if (cleanHex.isEmpty()) return ZERO

            // 每 8 个十六进制字符 = 1 个 UInt (32位)
            val numDigits = (cleanHex.length + 7) / 8
            val result = UIntArray(numDigits)

            var hexIndex = cleanHex.length
            var digitIndex = 0

            while (hexIndex > 0) {
                val start = maxOf(0, hexIndex - 8)
                val chunk = cleanHex.substring(start, hexIndex)
                result[digitIndex++] = chunk.toUInt(16)
                hexIndex = start
            }

            return CheckinBigInt(result.trimLeadingZeros(), false)
        }

        /**
         * 从 ASCII 字节数组创建 BigInt
         * 模拟 Python: message_int = 0; for byte in bytes: message_int = (message_int << 8) + byte
         */
        fun fromAsciiBytes(bytes: ByteArray): CheckinBigInt {
            if (bytes.isEmpty()) return ZERO

            var result = ZERO
            val shift8 = CheckinBigInt(uintArrayOf(256u), false)

            for (byte in bytes) {
                result =
                    result * shift8 + CheckinBigInt(uintArrayOf(byte.toUInt() and 0xFFu), false)
            }

            return result
        }
    }

    fun bitLength(): Int {
        if (isZero()) return 0
        val topDigit = digits.last()
        val topDigitBits = 32 - topDigit.countLeadingZeroBits()
        return (digits.size - 1) * 32 + topDigitBits
    }

    fun isZero(): Boolean = digits.size == 1 && digits[0] == 0u

    /**
     * 转换为十六进制字符串
     */
    fun toHexString(): String {
        if (isZero()) return "0"

        val sb = StringBuilder()
        for (i in digits.size - 1 downTo 0) {
            val hex = digits[i].toString(16)
            if (i == digits.size - 1) {
                sb.append(hex)  // 最高位不需要补零
            } else {
                sb.append(hex.padStart(8, '0'))  // 其他位补零到8位
            }
        }
        return sb.toString()
    }

    operator fun compareTo(other: CheckinBigInt): Int {
        if (digits.size != other.digits.size) {
            return digits.size.compareTo(other.digits.size)
        }
        for (i in digits.size - 1 downTo 0) {
            if (digits[i] != other.digits[i]) {
                return digits[i].compareTo(other.digits[i])
            }
        }
        return 0
    }

    operator fun plus(other: CheckinBigInt): CheckinBigInt {
        val maxLen = maxOf(digits.size, other.digits.size)
        val result = UIntArray(maxLen + 1)
        var carry = 0UL

        for (i in 0 until maxLen) {
            val a = if (i < digits.size) digits[i].toULong() else 0UL
            val b = if (i < other.digits.size) other.digits[i].toULong() else 0UL
            val sum = a + b + carry
            result[i] = sum.toUInt()
            carry = sum shr BASE_BITS
        }
        result[maxLen] = carry.toUInt()

        return CheckinBigInt(result.trimLeadingZeros(), false)
    }

    operator fun minus(other: CheckinBigInt): CheckinBigInt {
        if (this < other) {
            throw ArithmeticException("Negative result")
        }

        val result = UIntArray(digits.size)
        var borrow = 0L

        for (i in digits.indices) {
            val a = digits[i].toLong()
            val b = if (i < other.digits.size) other.digits[i].toLong() else 0L
            var diff = a - b - borrow
            if (diff < 0) {
                diff += BASE.toLong()
                borrow = 1
            } else {
                borrow = 0
            }
            result[i] = diff.toUInt()
        }

        return CheckinBigInt(result.trimLeadingZeros(), false)
    }

    operator fun times(other: CheckinBigInt): CheckinBigInt {
        val result = UIntArray(digits.size + other.digits.size)

        for (i in digits.indices) {
            var carry = 0UL
            for (j in other.digits.indices) {
                val product = digits[i].toULong() * other.digits[j].toULong() +
                        result[i + j].toULong() + carry
                result[i + j] = product.toUInt()
                carry = product shr BASE_BITS
            }
            result[i + other.digits.size] = carry.toUInt()
        }

        return CheckinBigInt(result.trimLeadingZeros(), false)
    }

    operator fun rem(other: CheckinBigInt): CheckinBigInt {
        if (other.isZero()) throw ArithmeticException("Division by zero")
        if (this < other) return this
        if (this == other) return ZERO

        // 简单的减法实现取模
        var remainder = this
        val otherBitLen = other.bitLength()

        while (remainder >= other) {
            val shift = remainder.bitLength() - otherBitLen
            var shifted = if (shift > 0) other.shiftLeft(shift) else other
            if (shifted > remainder) {
                shifted = other.shiftLeft(shift - 1)
            }
            remainder = remainder - shifted
        }

        return remainder
    }

    fun shiftLeft(bits: Int): CheckinBigInt {
        if (bits == 0 || isZero()) return this

        val digitShift = bits / BASE_BITS
        val bitShift = bits % BASE_BITS

        val newSize = digits.size + digitShift + 1
        val result = UIntArray(newSize)

        if (bitShift == 0) {
            for (i in digits.indices) {
                result[i + digitShift] = digits[i]
            }
        } else {
            var carry = 0u
            for (i in digits.indices) {
                val newDigit = (digits[i] shl bitShift) or carry
                carry = digits[i] shr (BASE_BITS - bitShift)
                result[i + digitShift] = newDigit
            }
            result[digits.size + digitShift] = carry
        }

        return CheckinBigInt(result.trimLeadingZeros(), false)
    }

    /**
     * 模幂运算: this^exp mod m
     * 使用平方-乘法算法
     */
    fun modPow(exp: CheckinBigInt, mod: CheckinBigInt): CheckinBigInt {
        if (mod.isZero()) throw ArithmeticException("Modulus is zero")
        if (exp.isZero()) return ONE

        var base = this % mod
        var result = ONE
        var e = exp

        while (!e.isZero()) {
            if ((e.digits[0] and 1u) == 1u) {
                result = (result * base) % mod
            }
            base = (base * base) % mod
            e = e.shiftRight(1)
        }

        return result
    }

    private fun shiftRight(bits: Int): CheckinBigInt {
        if (bits == 0 || isZero()) return this

        val digitShift = bits / BASE_BITS
        if (digitShift >= digits.size) return ZERO

        val bitShift = bits % BASE_BITS
        val newSize = digits.size - digitShift
        val result = UIntArray(newSize)

        if (bitShift == 0) {
            for (i in 0 until newSize) {
                result[i] = digits[i + digitShift]
            }
        } else {
            for (i in 0 until newSize) {
                result[i] = digits[i + digitShift] shr bitShift
                if (i + digitShift + 1 < digits.size) {
                    result[i] = result[i] or (digits[i + digitShift + 1] shl (BASE_BITS - bitShift))
                }
            }
        }

        return CheckinBigInt(result.trimLeadingZeros(), false)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is CheckinBigInt) return false
        return digits.contentEquals(other.digits) && negative == other.negative
    }

    override fun hashCode(): Int = digits.contentHashCode()
}

private fun UIntArray.trimLeadingZeros(): UIntArray {
    var lastNonZero = size - 1
    while (lastNonZero > 0 && this[lastNonZero] == 0u) {
        lastNonZero--
    }
    return if (lastNonZero == size - 1) this else sliceArray(0..lastNonZero)
}
