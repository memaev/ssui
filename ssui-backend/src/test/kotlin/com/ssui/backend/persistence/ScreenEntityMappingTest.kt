package com.ssui.backend.persistence

import com.ssui.backend.TestJson
import com.ssui.backend.mapping.ScreenMapper
import com.ssui.backend.seed.SeedScreenReader
import org.bson.Document
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver
import org.springframework.data.mongodb.core.mapping.MongoMappingContext
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Exercises Spring Data's document mapping in-memory (no MongoDB) to prove the stored document
 * has exactly the spec shape: `_id` at the top level, `id` inside elements, no `_class` fields.
 */
class ScreenEntityMappingTest {

    private val converter = MappingMongoConverter(NoOpDbRefResolver.INSTANCE, MongoMappingContext()).apply {
        setTypeMapper(DefaultMongoTypeMapper(null)) // same as MongoConfig
        afterPropertiesSet()
    }
    private val mapper = ScreenMapper()
    private val seed = SeedScreenReader(TestJson.objectMapper).read()

    @Test
    fun `entity is written with _id at the top level and id inside elements`() {
        val document = Document()
        converter.write(mapper.toEntity(seed), document)

        assertEquals("3f0c1b2e-9a1d-4a4c-8f1e-1e2d3c4b5a60", document["_id"])
        assertEquals("home", document["name"])
        val root = document.get("root", Document::class.java)
        assertEquals("b1e7e9c0-0000-4000-8000-000000000001", root["id"])
        assertFalse(root.containsKey("_id"))
        assertFalse(document.containsKey("_class"))
        assertFalse(root.containsKey("_class"))
        @Suppress("UNCHECKED_CAST")
        val button = (root["children"] as List<Document>)[3]
        assertEquals("Tap me", button["textContent"])
        assertEquals("b1e7e9c0-0000-4000-8000-000000000007", button["id"])
    }

    @Test
    fun `the raw spec document (as inserted by hand in Compass) reads back into the entity`() {
        val rawJson = ClassPathResource(SeedScreenReader.HOME_SEED_PATH).inputStream.use { it.readBytes().decodeToString() }
        val document = Document.parse(rawJson)

        val entity = converter.read(ScreenEntity::class.java, document)

        assertEquals(mapper.toEntity(seed), entity)
        assertTrue(entity.root.children[3].children.isEmpty(), "leaf without a children key defaults to empty list")
    }

    @Test
    fun `write then read is lossless`() {
        val original = mapper.toEntity(seed)
        val document = Document()
        converter.write(original, document)

        assertEquals(original, converter.read(ScreenEntity::class.java, document))
    }
}
