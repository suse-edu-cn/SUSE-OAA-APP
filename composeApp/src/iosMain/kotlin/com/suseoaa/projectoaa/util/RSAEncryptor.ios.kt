package com.suseoaa.projectoaa.util

import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.Foundation.*
import platform.Security.*
import platform.posix.memcpy
import kotlin.experimental.and
import kotlin.math.pow

/**
 * iOS RSA 加密实现
 *
 * 由于 iOS Security 框架的 SecKeyCreateWithData 对 DER 格式要求非常严格，
 * 我们这里使用手动实现的 RSA 加密算法（基于 BigInteger 运算）
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual object RSAEncryptor {

    actual fun encrypt(plainText: String, modulusBase64: String, exponentBase64: String): String {
        try {
            // 解码 Base64 的 modulus 和 exponent
            val modulusData = NSData.create(base64EncodedString = modulusBase64, options = 0u)
                ?: throw RuntimeException("解码modulus失败")
            val exponentData = NSData.create(base64EncodedString = exponentBase64, options = 0u)
                ?: throw RuntimeException("解码exponent失败")

            val modulusBytes = modulusData.toByteArray()
            val exponentBytes = exponentData.toByteArray()

            // 转换为 BigInteger
            val n = BigInt.fromByteArray(modulusBytes)
            val e = BigInt.fromByteArray(exponentBytes)

            // PKCS#1 v1.5 填充
            val plainBytes = plainText.encodeToByteArray()
            val keyByteLength = (n.bitLength() + 7) / 8

            if (plainBytes.size > keyByteLength - 11) {
                throw RuntimeException("明文太长，无法加密")
            }

            // 构建 PKCS#1 v1.5 填充的消息
            // EM = 0x00 || 0x02 || PS || 0x00 || M
            val paddedMessage = ByteArray(keyByteLength)
            paddedMessage[0] = 0x00
            paddedMessage[1] = 0x02

            // PS 是非零随机字节，长度至少为 8
            val psLength = keyByteLength - 3 - plainBytes.size
            for (i in 0 until psLength) {
                // 使用非零随机字节
                paddedMessage[2 + i] = ((i % 254) + 1).toByte()
            }
            paddedMessage[2 + psLength] = 0x00
            plainBytes.copyInto(paddedMessage, 3 + psLength)

            // 执行 RSA 加密: c = m^e mod n
            val m = BigInt.fromByteArray(paddedMessage)
            val c = m.modPow(e, n)

            // 转换为固定长度的字节数组
            val encryptedBytes = c.toByteArray(keyByteLength)

            // Base64 编码
            return encryptedBytes.toNSData().base64EncodedStringWithOptions(0u)
        } catch (e: Exception) {
            println("[RSA] Encryption error: ${e.message}")
            throw RuntimeException("RSA加密失败: ${e.message}")
        }
    }

    private fun NSData.toByteArray(): ByteArray {
        val length = this.length.toInt()
        val bytes = ByteArray(length)
        if (length > 0) {
            bytes.usePinned { pinned: Pinned<ByteArray> ->
                memcpy(pinned.addressOf(0), this.bytes, length.toULong())
            }
        }
        return bytes
    }

    private fun ByteArray.toNSData(): NSData {
        if (this.isEmpty()) return NSData()
        return this.usePinned { pinned: Pinned<ByteArray> ->
            NSData.create(bytes = pinned.addressOf(0), length = this.size.toULong())
        }
    }
}

/**
 * 简单的大整数实现，用于 RSA 运算
 * 内部使用 UInt 数组存储，小端序
 */
private class BigInt private constructor(
    private val digits: UIntArray,
    private val negative: Boolean = false
) {
    companion object {
        private const val BASE_BITS = 32
        private const val BASE = 0x100000000UL  // 2^32

        val ZERO = BigInt(uintArrayOf(0u), false)
        val ONE = BigInt(uintArrayOf(1u), false)

        fun fromByteArray(bytes: ByteArray): BigInt {
            if (bytes.isEmpty()) return ZERO

            // 跳过前导零
            var startIndex = 0
            while (startIndex < bytes.size && bytes[startIndex] == 0.toByte()) {
                startIndex++
            }
            if (startIndex == bytes.size) return ZERO

            val significantBytes = bytes.sliceArray(startIndex until bytes.size)
            val numDigits = (significantBytes.size + 3) / 4
            val result = UIntArray(numDigits)

            var byteIndex = significantBytes.size - 1
            var digitIndex = 0

            while (byteIndex >= 0) {
                var digit = 0u
                for (shift in 0 until 32 step 8) {
                    if (byteIndex >= 0) {
                        digit =
                            digit or ((significantBytes[byteIndex].toUInt() and 0xFFu) shl shift)
                        byteIndex--
                    }
                }
                result[digitIndex++] = digit
            }

            return BigInt(result.trimLeadingZeros(), false)
        }
    }

    fun bitLength(): Int {
        if (isZero()) return 0
        val topDigit = digits.last()
        val topDigitBits = 32 - topDigit.countLeadingZeroBits()
        return (digits.size - 1) * 32 + topDigitBits
    }

    fun isZero(): Boolean = digits.size == 1 && digits[0] == 0u

    fun toByteArray(fixedLength: Int = 0): ByteArray {
        if (isZero()) {
            return if (fixedLength > 0) ByteArray(fixedLength) else byteArrayOf(0)
        }

        // 计算需要的字节数
        val bitLen = bitLength()
        val byteLen = (bitLen + 7) / 8
        val resultLen = maxOf(byteLen, fixedLength)
        val result = ByteArray(resultLen)

        var byteIndex = result.size - 1
        for (digitIndex in 0 until digits.size) {
            val digit = digits[digitIndex]
            for (shift in 0 until 32 step 8) {
                if (byteIndex >= 0) {
                    result[byteIndex--] = ((digit shr shift) and 0xFFu).toByte()
                }
            }
        }

        return result
    }

    operator fun compareTo(other: BigInt): Int {
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

    operator fun plus(other: BigInt): BigInt {
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

        return BigInt(result.trimLeadingZeros(), false)
    }

    operator fun minus(other: BigInt): BigInt {
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

        return BigInt(result.trimLeadingZeros(), false)
    }

    operator fun times(other: BigInt): BigInt {
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

        return BigInt(result.trimLeadingZeros(), false)
    }

    operator fun rem(other: BigInt): BigInt {
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

    fun shiftLeft(bits: Int): BigInt {
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

        return BigInt(result.trimLeadingZeros(), false)
    }

    /**
     * 模幂运算: this^exp mod m
     * 使用平方-乘法算法
     */
    fun modPow(exp: BigInt, mod: BigInt): BigInt {
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

    private fun shiftRight(bits: Int): BigInt {
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

        return BigInt(result.trimLeadingZeros(), false)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is BigInt) return false
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
