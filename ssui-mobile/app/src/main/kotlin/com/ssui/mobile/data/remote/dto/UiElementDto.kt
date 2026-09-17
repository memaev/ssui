package com.ssui.mobile.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Mirror of the backend `UiElementDto` (spec 4.3).
 *
 * Enum-like fields (`type`, `horizontalAlignment`, `verticalArrangement`, `mode`, action `type`) are
 * deliberately plain [String]s: a value the app does not know must not fail the whole document.
 * The mapper turns them into domain enums and drops only the offending element.
 */
@Serializable
data class UiElementDto(
    val id: String,
    val type: String,
    @Serializable(with = LenientUiElementListSerializer::class)
    val children: List<UiElementDto> = emptyList(),
    val textContent: String? = null,
    val imageContent: String? = null,
    val contentDescription: String? = null,
    val containerColor: String? = null,
    val contentColor: String? = null,
    val padding: PaddingDto? = null,
    val width: SizeDto? = null,
    val height: SizeDto? = null,
    val horizontalAlignment: String? = null,
    val verticalArrangement: String? = null,
    val spacing: Int? = null,
    val onClick: ActionDto? = null,
)

@Serializable
data class PaddingDto(
    val start: Int,
    val top: Int,
    val end: Int,
    val bottom: Int,
)

@Serializable
data class SizeDto(
    val mode: String,
    val dp: Int? = null,
)

@Serializable
data class ActionDto(
    val type: String,
    val payload: Map<String, String> = emptyMap(),
)
