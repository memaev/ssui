package com.ssui.backend.api

import com.ssui.backend.TestJson
import com.ssui.backend.seed.SeedScreenReader
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScreenValidatorTest {

    private val validator = ScreenValidator()
    private val seed = SeedScreenReader(TestJson.objectMapper).read()

    private fun element(
        type: ElementType,
        textContent: String? = null,
        imageContent: String? = null,
        width: SizeDto? = null,
        height: SizeDto? = null,
        children: List<UiElementDto> = emptyList(),
    ) = UiElementDto(
        id = "el-$type",
        type = type,
        textContent = textContent,
        imageContent = imageContent,
        width = width,
        height = height,
        children = children,
    )

    private fun screen(root: UiElementDto) = ScreenDto(id = "s", name = "s", root = root)

    @Test
    fun `seed document is valid`() {
        assertTrue(validator.collectErrors(seed).isEmpty())
        validator.validate(seed)
    }

    @Test
    fun `root must be a COLUMN`() {
        val errors = validator.collectErrors(screen(element(ElementType.ROW)))
        assertEquals(listOf("root.type must be COLUMN but was ROW"), errors)
    }

    @Test
    fun `FIXED size requires dp greater than zero`() {
        val root = element(
            ElementType.COLUMN,
            width = SizeDto(SizeMode.FIXED, dp = 0),
            height = SizeDto(SizeMode.FIXED, dp = null),
            children = listOf(element(ElementType.TEXT, textContent = "t", width = SizeDto(SizeMode.FIXED, dp = -5))),
        )

        val errors = validator.collectErrors(screen(root))

        assertEquals(
            listOf(
                "root.width.dp must be > 0 when mode is FIXED but was 0",
                "root.height.dp must be > 0 when mode is FIXED but was null",
                "root.children[0].width.dp must be > 0 when mode is FIXED but was -5",
            ),
            errors,
        )
    }

    @Test
    fun `FILL and WRAP sizes do not need dp`() {
        val root = element(ElementType.COLUMN, width = SizeDto(SizeMode.FILL), height = SizeDto(SizeMode.WRAP))
        assertTrue(validator.collectErrors(screen(root)).isEmpty())
    }

    @Test
    fun `IMAGE requires imageContent`() {
        val root = element(
            ElementType.COLUMN,
            children = listOf(element(ElementType.IMAGE), element(ElementType.IMAGE, imageContent = "  ")),
        )

        val errors = validator.collectErrors(screen(root))

        assertEquals(
            listOf(
                "root.children[0].imageContent is required for IMAGE",
                "root.children[1].imageContent is required for IMAGE",
            ),
            errors,
        )
    }

    @Test
    fun `TEXT and BUTTON require textContent`() {
        val root = element(
            ElementType.COLUMN,
            children = listOf(
                element(ElementType.ROW, children = listOf(element(ElementType.TEXT))),
                element(ElementType.BUTTON),
            ),
        )

        val errors = validator.collectErrors(screen(root))

        assertEquals(
            listOf(
                "root.children[0].children[0].textContent is required for TEXT",
                "root.children[1].textContent is required for BUTTON",
            ),
            errors,
        )
    }

    @Test
    fun `validate throws with all violations joined in the message`() {
        val root = element(ElementType.ROW, children = listOf(element(ElementType.IMAGE)))

        val ex = assertThrows<ScreenValidationException> { validator.validate(screen(root)) }

        assertEquals(2, ex.errors.size)
        assertEquals(
            "Invalid screen: root.type must be COLUMN but was ROW; root.children[0].imageContent is required for IMAGE",
            ex.message,
        )
    }
}
