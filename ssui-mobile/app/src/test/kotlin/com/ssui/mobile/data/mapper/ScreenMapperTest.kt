package com.ssui.mobile.data.mapper

import com.ssui.mobile.data.remote.SsuiJson
import com.ssui.mobile.data.remote.dto.ActionDto
import com.ssui.mobile.data.remote.dto.PaddingDto
import com.ssui.mobile.data.remote.dto.ScreenDto
import com.ssui.mobile.data.remote.dto.SizeDto
import com.ssui.mobile.data.remote.dto.UiElementDto
import com.ssui.mobile.domain.ActionType
import com.ssui.mobile.domain.ElementType
import com.ssui.mobile.domain.HorizontalAlignment
import com.ssui.mobile.domain.Padding
import com.ssui.mobile.domain.Size
import com.ssui.mobile.domain.VerticalArrangement
import com.ssui.mobile.testutil.RecordingLogger
import com.ssui.mobile.testutil.SEED_HOME_JSON
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ScreenMapperTest {

    private lateinit var logger: RecordingLogger
    private lateinit var mapper: ScreenMapper

    @Before
    fun setUp() {
        logger = RecordingLogger()
        mapper = ScreenMapper(logger)
    }

    @Test
    fun `maps the seed home screen from spec 4_5`() {
        val dto = SsuiJson.decodeFromString(ScreenDto.serializer(), SEED_HOME_JSON)

        val screen = mapper.toDomain(dto)

        assertEquals("3f0c1b2e-9a1d-4a4c-8f1e-1e2d3c4b5a60", screen.id)
        assertEquals("home", screen.name)

        val root = screen.root
        assertEquals(ElementType.COLUMN, root.type)
        assertEquals("#FFF5F5F5", root.containerColor)
        assertEquals(Padding(16, 16, 16, 16), root.padding)
        assertEquals(HorizontalAlignment.CENTER, root.horizontalAlignment)
        assertEquals(VerticalArrangement.TOP, root.verticalArrangement)
        assertEquals(12, root.spacing)
        assertEquals(Size.Fill, root.width)   // default for COLUMN
        assertEquals(Size.Wrap, root.height)  // default
        assertEquals(4, root.children.size)

        val row = root.children[0]
        assertEquals(ElementType.ROW, row.type)
        assertEquals(VerticalArrangement.CENTER, row.verticalArrangement)
        assertEquals(8, row.spacing)
        assertEquals(2, row.children.size)

        val avatar = row.children[0]
        assertEquals(ElementType.IMAGE, avatar.type)
        assertEquals("https://picsum.photos/id/237/200/200", avatar.imageContent)
        assertEquals("Avatar", avatar.contentDescription)
        assertEquals(Size.Fixed(48), avatar.width)
        assertEquals(Size.Fixed(48), avatar.height)

        val welcome = row.children[1]
        assertEquals(ElementType.TEXT, welcome.type)
        assertEquals("Welcome to SSUI", welcome.textContent)
        assertEquals("#FFFFFFFF", welcome.contentColor)
        assertEquals(Size.Wrap, welcome.width)  // default for leaves

        val banner = root.children[1]
        assertEquals(Size.Fill, banner.width)
        assertEquals(Size.Fixed(180), banner.height)

        val button = root.children[3]
        assertEquals(ElementType.BUTTON, button.type)
        assertEquals("Tap me", button.textContent)
        assertNotNull(button.onClick)
        assertEquals(ActionType.SHOW_TOAST, button.onClick!!.type)
        assertEquals(mapOf("message" to "Hello from the server!"), button.onClick!!.payload)

        assertTrue("no warnings expected for a valid document, got ${logger.warnings}", logger.warnings.isEmpty())
    }

    @Test
    fun `element with unknown type is dropped and logged, siblings survive`() {
        val root = column(
            text("t1", "first"),
            UiElementDto(id = "carousel-1", type = "CAROUSEL", textContent = "future component"),
            text("t2", "last"),
        )

        val screen = mapper.toDomain(ScreenDto("s", "home", root))

        assertEquals(listOf("t1", "t2"), screen.root.children.map { it.id })
        assertTrue(logger.warnings.any { it.contains("carousel-1") && it.contains("CAROUSEL") })
    }

    @Test
    fun `unknown action type is ignored and logged but the element is kept`() {
        val root = column(
            UiElementDto(
                id = "btn",
                type = "BUTTON",
                textContent = "Vibrate",
                onClick = ActionDto(type = "VIBRATE", payload = mapOf("ms" to "200")),
            ),
        )

        val screen = mapper.toDomain(ScreenDto("s", "home", root))

        val button = screen.root.children.single()
        assertEquals(ElementType.BUTTON, button.type)
        assertEquals("Vibrate", button.textContent)
        assertNull(button.onClick)
        assertTrue(logger.warnings.any { it.contains("VIBRATE") && it.contains("btn") })
    }

    @Test
    fun `known action types are mapped with their payload`() {
        val root = column(
            UiElementDto(id = "b1", type = "BUTTON", textContent = "x", onClick = ActionDto("OPEN_URL", mapOf("url" to "https://example.com"))),
            UiElementDto(id = "b2", type = "BUTTON", textContent = "y", onClick = ActionDto("NAVIGATE", mapOf("screen" to "details"))),
        )

        val children = mapper.toDomain(ScreenDto("s", "home", root)).root.children

        assertEquals(ActionType.OPEN_URL, children[0].onClick?.type)
        assertEquals("https://example.com", children[0].onClick?.payload?.get("url"))
        assertEquals(ActionType.NAVIGATE, children[1].onClick?.type)
    }

    @Test
    fun `element with invalid enum value elsewhere is dropped`() {
        val root = column(
            UiElementDto(id = "bad-row", type = "ROW", horizontalAlignment = "DIAGONAL", children = listOf(text("inner", "x"))),
            UiElementDto(id = "bad-col", type = "COLUMN", verticalArrangement = "SPIRAL"),
            text("ok", "still here"),
        )

        val screen = mapper.toDomain(ScreenDto("s", "home", root))

        assertEquals(listOf("ok"), screen.root.children.map { it.id })
        assertTrue(logger.warnings.any { it.contains("bad-row") && it.contains("DIAGONAL") })
        assertTrue(logger.warnings.any { it.contains("bad-col") && it.contains("SPIRAL") })
    }

    @Test
    fun `invalid sizes drop the element`() {
        val root = column(
            UiElementDto(id = "no-dp", type = "IMAGE", imageContent = "u", height = SizeDto(mode = "FIXED")),
            UiElementDto(id = "zero-dp", type = "IMAGE", imageContent = "u", height = SizeDto(mode = "FIXED", dp = 0)),
            UiElementDto(id = "bad-mode", type = "IMAGE", imageContent = "u", height = SizeDto(mode = "STRETCH")),
            UiElementDto(id = "good", type = "IMAGE", imageContent = "u", height = SizeDto(mode = "FIXED", dp = 100)),
        )

        val screen = mapper.toDomain(ScreenDto("s", "home", root))

        assertEquals(listOf("good"), screen.root.children.map { it.id })
        assertEquals(Size.Fixed(100), screen.root.children.single().height)
        assertEquals(3, logger.warnings.size)
    }

    @Test
    fun `negative padding drops the element`() {
        val root = column(
            UiElementDto(id = "neg", type = "TEXT", textContent = "x", padding = PaddingDto(-1, 0, 0, 0)),
            text("ok", "y"),
        )

        val screen = mapper.toDomain(ScreenDto("s", "home", root))

        assertEquals(listOf("ok"), screen.root.children.map { it.id })
    }

    @Test
    fun `defaults are applied per spec`() {
        val root = column(
            UiElementDto(id = "row", type = "ROW"),
            UiElementDto(id = "txt", type = "TEXT", textContent = "t"),
            UiElementDto(id = "btn", type = "BUTTON", textContent = "b"),
            UiElementDto(id = "img", type = "IMAGE", imageContent = "u"),
        )

        val children = mapper.toDomain(ScreenDto("s", "home", root)).root.children

        assertEquals(Size.Fill, children[0].width)
        assertEquals(Size.Wrap, children[1].width)
        assertEquals(Size.Wrap, children[2].width)
        assertEquals(Size.Wrap, children[3].width)
        children.forEach {
            assertEquals(Size.Wrap, it.height)
            assertEquals(HorizontalAlignment.START, it.horizontalAlignment)
            assertEquals(VerticalArrangement.TOP, it.verticalArrangement)
            assertEquals(0, it.spacing)
            assertNull(it.padding)
            assertNull(it.onClick)
        }
    }

    @Test
    fun `enum parsing is tolerant to case and whitespace`() {
        val root = UiElementDto(id = "root", type = " column ", horizontalAlignment = "center", width = SizeDto("fill"))

        val screen = mapper.toDomain(ScreenDto("s", "home", root))

        assertEquals(ElementType.COLUMN, screen.root.type)
        assertEquals(HorizontalAlignment.CENTER, screen.root.horizontalAlignment)
        assertEquals(Size.Fill, screen.root.width)
    }

    @Test
    fun `root with unknown type cannot be mapped`() {
        val dto = ScreenDto("s", "home", UiElementDto(id = "root", type = "GRID"))

        assertThrows(IllegalArgumentException::class.java) { mapper.toDomain(dto) }
    }

    private fun column(vararg children: UiElementDto) = UiElementDto(id = "root", type = "COLUMN", children = children.toList())

    private fun text(id: String, text: String) = UiElementDto(id = id, type = "TEXT", textContent = text)
}
