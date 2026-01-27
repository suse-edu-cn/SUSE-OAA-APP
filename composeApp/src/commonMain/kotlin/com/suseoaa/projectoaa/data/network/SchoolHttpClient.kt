package com.suseoaa.projectoaa.data.network

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object SchoolHttpClient {
    fun create(json: Json): HttpClient {
        return HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = 10000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 10000
            }

            install(ContentNegotiation) {
                json(json)
            }

            install(Logging) {
                level = LogLevel.INFO
                logger = Logger.DEFAULT
            }
            
            install(HttpCookies) {
                storage = AcceptAllCookiesStorage()
            }
            
            // Allow redirects but we might need to handle 302 manually for auth
            followRedirects = false 
        }
    }
}
