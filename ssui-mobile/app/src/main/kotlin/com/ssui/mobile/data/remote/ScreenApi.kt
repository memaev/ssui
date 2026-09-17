package com.ssui.mobile.data.remote

import com.ssui.mobile.data.remote.dto.ScreenDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import java.io.IOException

/** `GET {baseUrl}/api/v1/screens/{name}` (spec 5.3). */
class ScreenApi(
    private val client: HttpClient,
    baseUrl: String,
) {
    private val baseUrl = baseUrl.trimEnd('/')

    suspend fun getScreen(name: String): ScreenDto {
        val url = "$baseUrl/api/v1/screens/$name"
        try {
            return client.get(url).body()
        } catch (e: ResponseException) {
            val status = e.response.status
            throw IOException("HTTP ${status.value} ${status.description} from $url", e)
        }
    }
}
