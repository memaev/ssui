package com.ssui.mobile.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json

private const val TIMEOUT_MS = 10_000L

/** Ktor client on the OkHttp engine with kotlinx.serialization content negotiation (spec 6.1). */
fun createHttpClient(): HttpClient = HttpClient(OkHttp) {
    expectSuccess = true
    install(ContentNegotiation) {
        json(SsuiJson)
    }
    install(HttpTimeout) {
        connectTimeoutMillis = TIMEOUT_MS
        requestTimeoutMillis = TIMEOUT_MS
        socketTimeoutMillis = TIMEOUT_MS
    }
}
