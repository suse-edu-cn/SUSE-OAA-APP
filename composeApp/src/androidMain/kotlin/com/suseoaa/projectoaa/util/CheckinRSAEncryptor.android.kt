package com.suseoaa.projectoaa.util

import java.math.BigInteger

/**
 * 打卡系统专用 RSA 加密器 - Android 实现
 * 模拟 Python Auto_SUSE_By_Password.py 的 rsa_encrypt_custom 函数
 */
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
            val messageBytes = plainText.toByteArray(Charsets.US_ASCII)
            var messageInt = BigInteger.ZERO
            for (byte in messageBytes) {
                messageInt = messageInt.shiftLeft(8).add(BigInteger.valueOf(byte.toLong() and 0xFF))
            }
            
            // 将十六进制的 modulus 和 exponent 转为 BigInteger
            val modulusInt = BigInteger(modulusHex, 16)
            val exponentInt = BigInteger(exponentHex, 16)
            
            // 执行 RSA 加密: encrypted = message^exponent mod modulus
            val encryptedInt = messageInt.modPow(exponentInt, modulusInt)
            
            // 转为十六进制字符串，补零到 256 位（1024位密钥）
            val encryptedHex = encryptedInt.toString(16)
            return encryptedHex.padStart(256, '0')
        } catch (e: Exception) {
            throw RuntimeException("RSA加密失败: ${e.message}", e)
        }
    }
}
