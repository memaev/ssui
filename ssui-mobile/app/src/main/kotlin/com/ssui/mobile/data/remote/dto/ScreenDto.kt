package com.ssui.mobile.data.remote.dto

import kotlinx.serialization.Serializable

/** Mirror of the backend `ScreenDto` (spec 4.2). */
@Serializable
data class ScreenDto(
    val id: String,
    val name: String,
    val root: UiElementDto,
)
