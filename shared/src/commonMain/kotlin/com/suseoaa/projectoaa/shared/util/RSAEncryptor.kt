package com.suseoaa.projectoaa.shared.util

expect object RSAEncryptor {
    fun encrypt(plainText: String, modulusBase64: String, exponentBase64: String): String
}
