package com.ssui.backend.seed

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.ssui.backend.api.ScreenDto
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

/**
 * Reads a seed file written in MongoDB document form (with `_id`) into a [ScreenDto] (with `id`).
 * Keeping the seed in Mongo form means `seed/home.json` is byte-for-byte the spec example (4.5).
 */
@Component
class SeedScreenReader(private val objectMapper: ObjectMapper) {

    fun read(resource: Resource = ClassPathResource(HOME_SEED_PATH)): ScreenDto {
        val node = resource.inputStream.use { objectMapper.readTree(it) }
        require(node is ObjectNode) { "Seed ${resource.description} must be a JSON object" }
        if (node.has(MONGO_ID)) {
            node.set<JsonNode>(DTO_ID, node.remove(MONGO_ID))
        }
        return objectMapper.treeToValue(node, ScreenDto::class.java)
    }

    companion object {
        const val HOME_SEED_PATH = "seed/home.json"
        private const val MONGO_ID = "_id"
        private const val DTO_ID = "id"
    }
}
