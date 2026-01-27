package com.suseoaa.projectoaa.util

expect object RSAEncryptor {
    fun encrypt(plainText: String, modulusBase64: String, exponentBase64: String): String
}
