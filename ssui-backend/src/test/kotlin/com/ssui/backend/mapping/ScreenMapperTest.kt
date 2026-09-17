package com.ssui.backend.mapping

import com.fasterxml.jackson.databind.node.ObjectNode
import com.ssui.backend.TestJson
import com.ssui.backend.api.ElementType
import com.ssui.backend.api.ScreenDto
import com.ssui.backend.api.SizeMode
import com.ssui.backend.persistence.ScreenEntity
import com.ssui.backend.seed.SeedScreenReader
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ScreenMapperTest {

    private val json = TestJson.objectMapper
    private val mapper = ScreenMapper()
    private val seed: ScreenDto = SeedScreenReader(json).read()

    @Test
    fun `seed _id is mapped to the entity id`() {
        val entity = mapper.toEntity(seed)

        assertEquals("3f0c1b2e-9a1d-4a4c-8f1e-1e2d3c4b5a60", entity.id)
        assertEquals("home", entity.name)
    }

    @Test
    fun `dto to entity to dto round trip of the seed document is lossless`() {
        val entity: ScreenEntity = mapper.toEntity(seed)
        val roundTripped: ScreenDto = mapper.toDto(entity)

        assertEquals(seed, roundTripped)
    }

    @Test
    fun `entity carries the full tree with all field values`() {
        val root = mapper.toEntity(seed).root

        assertEquals(ElementType.COLUMN, root.type)
        assertEquals("#FFF5F5F5", root.containerColor)
        assertEquals(16, root.padding?.start)
        assertEquals(12, root.spacing)
        assertEquals(4, root.children.size)

        val avatar = root.children[0].children[0]
        assertEquals(ElementType.IMAGE, avatar.type)
        assertEquals(SizeMode.FIXED, avatar.width?.mode)
        assertEquals(48, avatar.height?.dp)
        assertEquals("Avatar", avatar.contentDescription)

        val button = root.children[3]
        assertEquals(ElementType.BUTTON, button.type)
        assertEquals("Tap me", button.textContent)
        assertEquals("Hello from the server!", button.onClick?.payload?.get("message"))
        assertTrue(button.children.isEmpty())
    }

    @Test
    fun `serialized dto always contains children and omits null fields`() {
        val dto = mapper.toDto(mapper.toEntity(seed))
        val tree = json.valueToTree<ObjectNode>(dto)

        assertEquals("3f0c1b2e-9a1d-4a4c-8f1e-1e2d3c4b5a60", tree["id"].asText())
        val button = tree["root"]["children"][3]
        assertNotNull(button["children"], "leaf elements must still expose children")
        assertTrue(button["children"].isArray && button["children"].isEmpty)
        assertTrue(!button.has("imageContent"), "null fields are omitted")
        assertEquals("Tap me", button["textContent"].asText())
    }

    @Test
    fun `json to dto to json round trip equals the seed once _id is renamed and children are normalised`() {
        val expected = ClassPathResource(SeedScreenReader.HOME_SEED_PATH).inputStream.use { json.readTree(it) } as ObjectNode
        expected.set<ObjectNode>("id", expected.remove("_id"))
        addEmptyChildren(expected["root"] as ObjectNode)

        val actual = json.valueToTree<ObjectNode>(mapper.toDto(mapper.toEntity(seed)))

        assertEquals(expected, actual)
    }

    private fun addEmptyChildren(element: ObjectNode) {
        if (!element.has("children")) element.putArray("children")
        element["children"].forEach { addEmptyChildren(it as ObjectNode) }
    }
}
