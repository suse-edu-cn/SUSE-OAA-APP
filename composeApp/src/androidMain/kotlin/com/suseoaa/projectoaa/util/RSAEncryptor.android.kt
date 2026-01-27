package com.suseoaa.projectoaa.util

import java.math.BigInteger
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.Base64
import javax.crypto.Cipher

actual object RSAEncryptor {
    actual fun encrypt(plainText: String, modulusBase64: String, exponentBase64: String): String {
        try {
            val modulusBytes = Base64.getDecoder().decode(modulusBase64)
            val exponentBytes = Base64.getDecoder().decode(exponentBase64)

            val modulus = BigInteger(1, modulusBytes)
            val exponent = BigInteger(1, exponentBytes)

            val keySpec = RSAPublicKeySpec(modulus, exponent)
            val keyFactory = KeyFactory.getInstance("RSA")
            val publicKey: PublicKey = keyFactory.generatePublic(keySpec)

            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, publicKey)

            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            return Base64.getEncoder().encodeToString(encryptedBytes)
        } catch (e: Exception) {
            throw RuntimeException("RSA加密失败: ${e.message}", e)
        }
    }
}
