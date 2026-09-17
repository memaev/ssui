package com.ssui.backend.api

import org.springframework.stereotype.Component

/**
 * Structural validation applied to every screen that is written (PUT and seed).
 *
 * Rules (spec 5.3):
 *  - `root.type` must be COLUMN
 *  - every Size with mode FIXED must have `dp > 0`
 *  - IMAGE must have `imageContent`
 *  - TEXT and BUTTON must have `textContent`
 */
@Component
class ScreenValidator {

    /** Throws [ScreenValidationException] listing every violation found. */
    fun validate(screen: ScreenDto) {
        val errors = collectErrors(screen)
        if (errors.isNotEmpty()) {
            throw ScreenValidationException(errors)
        }
    }

    /** Returns all violations; empty list means the screen is valid. */
    fun collectErrors(screen: ScreenDto): List<String> {
        val errors = mutableListOf<String>()
        if (screen.root.type != ElementType.COLUMN) {
            errors += "root.type must be COLUMN but was ${screen.root.type}"
        }
        validateElement(screen.root, "root", errors)
        return errors
    }

    private fun validateElement(element: UiElementDto, path: String, errors: MutableList<String>) {
        validateSize(element.width, "$path.width", errors)
        validateSize(element.height, "$path.height", errors)

        when (element.type) {
            ElementType.IMAGE ->
                if (element.imageContent.isNullOrBlank()) {
                    errors += "$path.imageContent is required for IMAGE"
                }

            ElementType.TEXT, ElementType.BUTTON ->
                if (element.textContent == null) {
                    errors += "$path.textContent is required for ${element.type}"
                }

            ElementType.COLUMN, ElementType.ROW -> Unit
        }

        element.children.forEachIndexed { index, child ->
            validateElement(child, "$path.children[$index]", errors)
        }
    }

    private fun validateSize(size: SizeDto?, path: String, errors: MutableList<String>) {
        if (size == null || size.mode != SizeMode.FIXED) return
        val dp = size.dp
        if (dp == null || dp <= 0) {
            errors += "$path.dp must be > 0 when mode is FIXED but was $dp"
        }
    }
}

class ScreenValidationException(val errors: List<String>) :
    RuntimeException("Invalid screen: " + errors.joinToString("; "))
