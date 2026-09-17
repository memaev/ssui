package com.ssui.mobile.data.mapper

import com.ssui.mobile.data.remote.dto.ActionDto
import com.ssui.mobile.data.remote.dto.PaddingDto
import com.ssui.mobile.data.remote.dto.ScreenDto
import com.ssui.mobile.data.remote.dto.SizeDto
import com.ssui.mobile.data.remote.dto.UiElementDto
import com.ssui.mobile.domain.Action
import com.ssui.mobile.domain.ActionType
import com.ssui.mobile.domain.ElementType
import com.ssui.mobile.domain.HorizontalAlignment
import com.ssui.mobile.domain.Logger
import com.ssui.mobile.domain.Padding
import com.ssui.mobile.domain.Screen
import com.ssui.mobile.domain.Size
import com.ssui.mobile.domain.UiElement
import com.ssui.mobile.domain.VerticalArrangement

/**
 * DTO -> domain mapping with the robustness rules from spec 4.1 / 6.4:
 * - an element with an unknown `type`, or an invalid enum / size / padding value, is dropped and logged;
 *   its siblings still render;
 * - an unknown action type is logged and the element keeps rendering without `onClick`;
 * - defaults are applied: width FILL for COLUMN/ROW and WRAP otherwise, height WRAP,
 *   horizontalAlignment START, verticalArrangement TOP, spacing 0.
 */
class ScreenMapper(private val logger: Logger = Logger.None) {

    fun toDomain(dto: ScreenDto): Screen {
        val root = toDomainOrNull(dto.root)
            ?: throw IllegalArgumentException("Root element '${dto.root.id}' of screen '${dto.name}' is invalid")
        if (root.type != ElementType.COLUMN) {
            logger.w(TAG, "Root of screen '${dto.name}' is ${root.type}, expected COLUMN; rendering anyway")
        }
        return Screen(id = dto.id, name = dto.name, root = root)
    }

    /** Returns null (and logs) when the element cannot be represented in the domain. */
    fun toDomainOrNull(dto: UiElementDto): UiElement? {
        val type = parseEnumOrNull<ElementType>(dto.type)
            ?: return drop(dto, "unknown element type '${dto.type}'")

        val horizontalAlignment = dto.horizontalAlignment?.let { raw ->
            parseEnumOrNull<HorizontalAlignment>(raw) ?: return drop(dto, "unknown horizontalAlignment '$raw'")
        } ?: HorizontalAlignment.START

        val verticalArrangement = dto.verticalArrangement?.let { raw ->
            parseEnumOrNull<VerticalArrangement>(raw) ?: return drop(dto, "unknown verticalArrangement '$raw'")
        } ?: VerticalArrangement.TOP

        val width = dto.width?.let { toSizeOrNull(it) ?: return drop(dto, "invalid width $it") }
            ?: defaultWidth(type)
        val height = dto.height?.let { toSizeOrNull(it) ?: return drop(dto, "invalid height $it") }
            ?: Size.Wrap

        val padding = dto.padding?.let { toPaddingOrNull(it) ?: return drop(dto, "negative padding $it") }

        val spacing = (dto.spacing ?: 0).let { raw ->
            if (raw < 0) {
                logger.w(TAG, "Element ${dto.id}: negative spacing $raw coerced to 0")
                0
            } else raw
        }

        val onClick = dto.onClick?.let { toActionOrNull(it, dto.id) }

        val children = if (type.isContainer) dto.children.mapNotNull(::toDomainOrNull) else emptyList()
        if (!type.isContainer && dto.children.isNotEmpty()) {
            logger.w(TAG, "Element ${dto.id} ($type) has children but is a leaf; children ignored")
        }

        return UiElement(
            id = dto.id,
            type = type,
            children = children,
            textContent = dto.textContent,
            imageContent = dto.imageContent,
            contentDescription = dto.contentDescription,
            containerColor = dto.containerColor,
            contentColor = dto.contentColor,
            padding = padding,
            width = width,
            height = height,
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = verticalArrangement,
            spacing = spacing,
            onClick = onClick,
        )
    }

    fun toSizeOrNull(dto: SizeDto): Size? {
        val mode = dto.mode.trim().uppercase()
        return when (mode) {
            "FILL" -> Size.Fill
            "WRAP" -> Size.Wrap
            "FIXED" -> dto.dp?.takeIf { it > 0 }?.let(Size::Fixed)
            else -> null
        }
    }

    fun toPaddingOrNull(dto: PaddingDto): Padding? {
        if (dto.start < 0 || dto.top < 0 || dto.end < 0 || dto.bottom < 0) return null
        return Padding(start = dto.start, top = dto.top, end = dto.end, bottom = dto.bottom)
    }

    /** Unknown action types are ignored (null) and logged (spec 4.4); the element itself is kept. */
    fun toActionOrNull(dto: ActionDto, elementId: String): Action? {
        val type = parseEnumOrNull<ActionType>(dto.type)
        if (type == null) {
            logger.w(TAG, "Element $elementId: unknown action type '${dto.type}' ignored")
            return null
        }
        return Action(type = type, payload = dto.payload)
    }

    private fun drop(dto: UiElementDto, reason: String): UiElement? {
        logger.w(TAG, "Dropping element ${dto.id}: $reason")
        return null
    }

    private fun defaultWidth(type: ElementType): Size =
        if (type.isContainer) Size.Fill else Size.Wrap

    private val ElementType.isContainer: Boolean
        get() = this == ElementType.COLUMN || this == ElementType.ROW

    private inline fun <reified E : Enum<E>> parseEnumOrNull(raw: String): E? {
        val normalized = raw.trim().uppercase()
        return enumValues<E>().firstOrNull { it.name == normalized }
    }

    private companion object {
        const val TAG = "ScreenMapper"
    }
}
