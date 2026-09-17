package com.ssui.backend.api

/** Discriminator of a [UiElementDto]. The mobile app skips unknown values silently. */
enum class ElementType { COLUMN, ROW, TEXT, BUTTON, IMAGE }

/** COLUMN: horizontal alignment of children. ROW: horizontal arrangement of children. */
enum class HorizontalAlignment { START, CENTER, END }

/** COLUMN: vertical arrangement of children. ROW: only TOP/CENTER/BOTTOM are meaningful (vertical alignment). */
enum class VerticalArrangement { TOP, CENTER, BOTTOM, SPACE_BETWEEN, SPACE_AROUND, SPACE_EVENLY }

/** `dp` of a [SizeDto] is required only for FIXED. */
enum class SizeMode { FILL, WRAP, FIXED }

/** onClick action types. NAVIGATE is parsed but not implemented in the POC. */
enum class ActionType { SHOW_TOAST, OPEN_URL, NAVIGATE }
