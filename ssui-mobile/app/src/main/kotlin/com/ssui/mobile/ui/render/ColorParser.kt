package com.ssui.mobile.ui.render

import android.util.Log
import androidx.compose.ui.graphics.Color

private const val TAG = "SsuiColor"

/**
 * Parses an ARGB hex string (`"#FF5C738A"`; `"#5C738A"` is accepted as opaque) into a [Color].
 * Returns null for `null` input and, with a log line, for invalid strings so callers fall back
 * to the Material theme colour (spec 4.1 / 6.4).
 */
fun String?.toColorOrNull(): Color? {
    if (this == null) return null
    val hex = trim().removePrefix("#")
    val argb: Long? = when (hex.length) {
        8 -> hex.toLongOrNull(16)
        6 -> hex.toLongOrNull(16)?.or(0xFF000000L)
        else -> null
    }
    if (argb == null) {
        Log.w(TAG, "Invalid colour '$this'; falling back to the theme colour")
        return null
    }
    return Color(argb)
}
