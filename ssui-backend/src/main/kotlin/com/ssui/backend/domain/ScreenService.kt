package com.ssui.backend.domain

import com.ssui.backend.api.ScreenDto
import com.ssui.backend.api.ScreenSummaryDto
import com.ssui.backend.mapping.ScreenMapper
import com.ssui.backend.persistence.ScreenRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ScreenService(
    private val screenRepository: ScreenRepository,
    private val screenMapper: ScreenMapper,
) {

    fun getByName(name: String): ScreenDto =
        screenRepository.findByName(name)?.let(screenMapper::toDto)
            ?: throw ScreenNotFoundException(name)

    fun listAll(): List<ScreenSummaryDto> =
        screenRepository.findAll().map(screenMapper::toSummary)

    /**
     * Creates or fully replaces the screen called [name].
     *
     * - The path [name] always wins over `screen.name`.
     * - If a screen with that name already exists its id is kept, so the Mongo `_id` is stable across edits.
     * - Otherwise a client-supplied non-blank id is honoured (lets you re-create a screen with a known id),
     *   and a fresh UUID is generated when the body carries no id.
     */
    fun upsert(name: String, screen: ScreenDto): ScreenDto {
        val existingId = screenRepository.findByName(name)?.id
        val id = existingId
            ?: screen.id?.takeIf { it.isNotBlank() }
            ?: UUID.randomUUID().toString()

        val entity = screenMapper.toEntity(screen.copy(id = id, name = name))
        return screenMapper.toDto(screenRepository.save(entity))
    }
}

class ScreenNotFoundException(name: String) : RuntimeException("Screen '$name' not found")
