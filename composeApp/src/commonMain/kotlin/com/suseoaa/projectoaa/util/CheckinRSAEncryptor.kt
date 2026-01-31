package com.suseoaa.projectoaa.util

/**
 * 打卡系统专用 RSA 加密器
 * 
 * 与教务系统 RSAEncryptor 不同，打卡系统使用：
 * - 十六进制格式的 modulus 和 exponent（不是 Base64）
 * - 原始 RSA 幂模运算（不使用 PKCS1Padding）
 * - 输出为 256 位十六进制字符串
 */
expect object CheckinRSAEncryptor {
    /**
     * 使用原始 RSA 幂模运算加密
     * @param plainText 明文（已反转的密码）
     * @param modulusHex 模数（十六进制字符串）
     * @param exponentHex 指数（十六进制字符串）
     * @return 加密后的十六进制字符串（256位，前面补零）
     */
    fun encrypt(plainText: String, modulusHex: String, exponentHex: String): String
}
