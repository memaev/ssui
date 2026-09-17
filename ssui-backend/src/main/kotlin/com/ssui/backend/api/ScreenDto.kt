package com.ssui.backend.api

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls

/**
 * Wire representation of a screen. Field names are identical to the persistence entity (POC decision).
 *
 * `id` and `name` are optional on input (PUT): the path `name` wins and the id is resolved by the service.
 * Both are always present on output.
 */
data class ScreenDto(
    val id: String? = null,
    val name: String? = null,
    val root: UiElementDto,
)

/** Compact `{ id, name }` view used by `GET /api/v1/screens`. */
data class ScreenSummaryDto(
    val id: String,
    val name: String,
)

/**
 * One universal element model: a `type` discriminator plus nullable type-specific fields.
 * Colors are ARGB hex strings (e.g. `#FF5C738A`); all sizes are in dp.
 *
 * `children` is always serialized as a list (empty for leaf types) and a JSON `null` is read as an empty list.
 */
data class UiElementDto(
    val id: String,
    val type: ElementType,
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    val children: List<UiElementDto> = emptyList(),
    val textContent: String? = null,
    val imageContent: String? = null,
    val contentDescription: String? = null,
    val containerColor: String? = null,
    val contentColor: String? = null,
    val padding: PaddingDto? = null,
    val width: SizeDto? = null,
    val height: SizeDto? = null,
    val horizontalAlignment: HorizontalAlignment? = null,
    val verticalArrangement: VerticalArrangement? = null,
    val spacing: Int? = null,
    val onClick: ActionDto? = null,
)

/** All four sides in dp; all required when the object is present. */
data class PaddingDto(
    val start: Int,
    val top: Int,
    val end: Int,
    val bottom: Int,
)

/** `dp` is required (and must be > 0) only when `mode` is FIXED. */
data class SizeDto(
    val mode: SizeMode,
    val dp: Int? = null,
)

/** onClick action. Payload keys depend on the type: `message`, `url` or `screen`. */
data class ActionDto(
    val type: ActionType,
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    val payload: Map<String, String> = emptyMap(),
)
