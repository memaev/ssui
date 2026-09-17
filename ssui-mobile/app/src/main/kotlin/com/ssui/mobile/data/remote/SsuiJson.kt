package com.ssui.mobile.data.remote

import kotlinx.serialization.json.Json

/** Single JSON configuration shared by the Ktor client and tests (spec 6.1). */
val SsuiJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
    coerceInputValues = true
}
