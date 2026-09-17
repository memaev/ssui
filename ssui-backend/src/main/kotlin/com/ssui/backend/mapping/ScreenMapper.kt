package com.ssui.backend.mapping

import com.ssui.backend.api.ActionDto
import com.ssui.backend.api.PaddingDto
import com.ssui.backend.api.ScreenDto
import com.ssui.backend.api.ScreenSummaryDto
import com.ssui.backend.api.SizeDto
import com.ssui.backend.api.UiElementDto
import com.ssui.backend.persistence.ActionEntity
import com.ssui.backend.persistence.PaddingEntity
import com.ssui.backend.persistence.ScreenEntity
import com.ssui.backend.persistence.SizeEntity
import com.ssui.backend.persistence.UiElementEntity
import org.springframework.stereotype.Component

/** Explicit, field-by-field mapping between the persistence entities and the API DTOs. */
@Component
class ScreenMapper {

    fun toDto(entity: ScreenEntity): ScreenDto = ScreenDto(
        id = entity.id,
        name = entity.name,
        root = toDto(entity.root),
    )

    fun toSummary(entity: ScreenEntity): ScreenSummaryDto = ScreenSummaryDto(
        id = entity.id,
        name = entity.name,
    )

    /** Requires `id` and `name` to be resolved already (the service does that before saving). */
    fun toEntity(dto: ScreenDto): ScreenEntity = ScreenEntity(
        id = requireNotNull(dto.id) { "Screen id must be resolved before mapping to an entity" },
        name = requireNotNull(dto.name) { "Screen name must be resolved before mapping to an entity" },
        root = toEntity(dto.root),
    )

    fun toDto(entity: UiElementEntity): UiElementDto = UiElementDto(
        id = entity.id,
        type = entity.type,
        children = entity.children.map(::toDto),
        textContent = entity.textContent,
        imageContent = entity.imageContent,
        contentDescription = entity.contentDescription,
        containerColor = entity.containerColor,
        contentColor = entity.contentColor,
        padding = entity.padding?.let(::toDto),
        width = entity.width?.let(::toDto),
        height = entity.height?.let(::toDto),
        horizontalAlignment = entity.horizontalAlignment,
        verticalArrangement = entity.verticalArrangement,
        spacing = entity.spacing,
        onClick = entity.onClick?.let(::toDto),
    )

    fun toEntity(dto: UiElementDto): UiElementEntity = UiElementEntity(
        id = dto.id,
        type = dto.type,
        children = dto.children.map(::toEntity),
        textContent = dto.textContent,
        imageContent = dto.imageContent,
        contentDescription = dto.contentDescription,
        containerColor = dto.containerColor,
        contentColor = dto.contentColor,
        padding = dto.padding?.let(::toEntity),
        width = dto.width?.let(::toEntity),
        height = dto.height?.let(::toEntity),
        horizontalAlignment = dto.horizontalAlignment,
        verticalArrangement = dto.verticalArrangement,
        spacing = dto.spacing,
        onClick = dto.onClick?.let(::toEntity),
    )

    fun toDto(entity: PaddingEntity): PaddingDto =
        PaddingDto(start = entity.start, top = entity.top, end = entity.end, bottom = entity.bottom)

    fun toEntity(dto: PaddingDto): PaddingEntity =
        PaddingEntity(start = dto.start, top = dto.top, end = dto.end, bottom = dto.bottom)

    fun toDto(entity: SizeEntity): SizeDto = SizeDto(mode = entity.mode, dp = entity.dp)

    fun toEntity(dto: SizeDto): SizeEntity = SizeEntity(mode = dto.mode, dp = dto.dp)

    fun toDto(entity: ActionEntity): ActionDto = ActionDto(type = entity.type, payload = entity.payload)

    fun toEntity(dto: ActionDto): ActionEntity = ActionEntity(type = dto.type, payload = dto.payload)
}
