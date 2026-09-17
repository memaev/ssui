package com.ssui.mobile.domain

/** Element kinds the renderer knows how to draw (spec 4.3). Unknown kinds never reach the domain. */
enum class ElementType { COLUMN, ROW, TEXT, BUTTON, IMAGE }

enum class HorizontalAlignment { START, CENTER, END }

enum class VerticalArrangement { TOP, CENTER, BOTTOM, SPACE_BETWEEN, SPACE_AROUND, SPACE_EVENLY }

enum class ActionType { SHOW_TOAST, OPEN_URL, NAVIGATE }

/** Padding in dp, all sides required (spec 4.3). */
data class Padding(val start: Int, val top: Int, val end: Int, val bottom: Int)

/** Size of one axis (spec 4.3 "Size"). */
sealed interface Size {
    data object Fill : Size
    data object Wrap : Size
    data class Fixed(val dp: Int) : Size
}

/** onClick action (spec 4.4). Payload keys depend on [type]. */
data class Action(
    val type: ActionType,
    val payload: Map<String, String> = emptyMap(),
)

/**
 * Universal, already validated UI element. Defaults from the spec are applied by the mapper,
 * so the renderer never has to deal with nulls for layout fields.
 */
data class UiElement(
    val id: String,
    val type: ElementType,
    val children: List<UiElement> = emptyList(),
    val textContent: String? = null,
    val imageContent: String? = null,
    val contentDescription: String? = null,
    /** ARGB hex string such as "#FF5C738A"; null = Material theme default. */
    val containerColor: String? = null,
    /** ARGB hex string; null = Material theme default. */
    val contentColor: String? = null,
    val padding: Padding? = null,
    val width: Size,
    val height: Size,
    val horizontalAlignment: HorizontalAlignment = HorizontalAlignment.START,
    val verticalArrangement: VerticalArrangement = VerticalArrangement.TOP,
    val spacing: Int = 0,
    val onClick: Action? = null,
)

data class Screen(
    val id: String,
    val name: String,
    val root: UiElement,
)
