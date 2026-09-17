package com.ssui.backend.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.ssui.backend.domain.ScreenNotFoundException
import com.ssui.backend.domain.ScreenService
import com.ssui.backend.seed.SeedScreenReader
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mockito.never
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put

@WebMvcTest(ScreenController::class)
@Import(ScreenValidator::class)
class ScreenControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var screenService: ScreenService

    private val seed: ScreenDto by lazy { SeedScreenReader(objectMapper).read() }

    @Test
    fun `GET screen returns 200 with the full tree`() {
        given(screenService.getByName("home")).willReturn(seed)

        mockMvc.get("/api/v1/screens/home")
            .andExpect {
                status { isOk() }
                content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
                jsonPath("$.id") { value("3f0c1b2e-9a1d-4a4c-8f1e-1e2d3c4b5a60") }
                jsonPath("$.name") { value("home") }
                jsonPath("$.root.type") { value("COLUMN") }
                jsonPath("$.root.padding.start") { value(16) }
                jsonPath("$.root.children") { isArray(); value(hasSize<Any>(4)) }
                jsonPath("$.root.children[0].children[0].type") { value("IMAGE") }
                jsonPath("$.root.children[0].children[0].width.dp") { value(48) }
                jsonPath("$.root.children[0].children[0].children") { isArray(); isEmpty() }
                jsonPath("$.root.children[3].textContent") { value("Tap me") }
                jsonPath("$.root.children[3].onClick.payload.message") { value("Hello from the server!") }
                jsonPath("$.root.children[3].imageContent") { doesNotExist() }
            }
    }

    @Test
    fun `GET unknown screen returns 404 with error body`() {
        given(screenService.getByName("unknown")).willThrow(ScreenNotFoundException("unknown"))

        mockMvc.get("/api/v1/screens/unknown")
            .andExpect {
                status { isNotFound() }
                content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
                jsonPath("$.status") { value(404) }
                jsonPath("$.message") { value("Screen 'unknown' not found") }
            }
    }

    @Test
    fun `GET screens lists id and name`() {
        given(screenService.listAll()).willReturn(listOf(ScreenSummaryDto("id-1", "home"), ScreenSummaryDto("id-2", "about")))

        mockMvc.get("/api/v1/screens")
            .andExpect {
                status { isOk() }
                jsonPath("$") { isArray(); value(hasSize<Any>(2)) }
                jsonPath("$[0].id") { value("id-1") }
                jsonPath("$[0].name") { value("home") }
                jsonPath("$[1].name") { value("about") }
            }
    }

    @Test
    fun `PUT valid screen validates and delegates to the service with the path name`() {
        val saved = seed.copy(name = "landing")
        given(screenService.upsert(eqArg("landing"), anyArg<ScreenDto>())).willReturn(saved)

        mockMvc.put("/api/v1/screens/landing") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(seed)
        }.andExpect {
            status { isOk() }
            jsonPath("$.name") { value("landing") }
            jsonPath("$.root.children[3].textContent") { value("Tap me") }
        }

        then(screenService).should().upsert(eqArg("landing"), anyArg<ScreenDto>())
    }

    @Test
    fun `PUT with root of type ROW returns 400`() {
        val body = objectMapper.valueToTree<ObjectNode>(seed)
        (body["root"] as ObjectNode).put("type", "ROW")

        mockMvc.put("/api/v1/screens/home") {
            contentType = MediaType.APPLICATION_JSON
            content = body.toString()
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.status") { value(400) }
            jsonPath("$.message") { value(containsString("root.type must be COLUMN but was ROW")) }
        }

        then(screenService).should(never()).upsert(anyArg(), anyArg())
    }

    @Test
    fun `PUT with FIXED size without positive dp returns 400`() {
        val body = objectMapper.valueToTree<ObjectNode>(seed)
        val image = body["root"]["children"][1] as ObjectNode
        (image["height"] as ObjectNode).put("dp", 0)

        mockMvc.put("/api/v1/screens/home") {
            contentType = MediaType.APPLICATION_JSON
            content = body.toString()
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.status") { value(400) }
            jsonPath("$.message") { value(containsString("root.children[1].height.dp must be > 0")) }
        }
    }

    @Test
    fun `PUT with IMAGE missing imageContent and TEXT missing textContent returns 400 listing both`() {
        val body = objectMapper.valueToTree<ObjectNode>(seed)
        (body["root"]["children"][1] as ObjectNode).remove("imageContent")
        (body["root"]["children"][2] as ObjectNode).remove("textContent")

        mockMvc.put("/api/v1/screens/home") {
            contentType = MediaType.APPLICATION_JSON
            content = body.toString()
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.status") { value(400) }
            jsonPath("$.message") { value(containsString("root.children[1].imageContent is required for IMAGE")) }
            jsonPath("$.message") { value(containsString("root.children[2].textContent is required for TEXT")) }
        }
    }

    @Test
    fun `PUT with malformed JSON returns 400 in the same error shape`() {
        mockMvc.put("/api/v1/screens/home") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name": "home", "root": """
        }.andExpect {
            status { isBadRequest() }
            content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
            jsonPath("$.status") { value(400) }
            jsonPath("$.message") { value(containsString("Malformed JSON request body")) }
        }
    }

    @Test
    fun `PUT with unknown enum value returns 400`() {
        mockMvc.put("/api/v1/screens/home") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"root": {"id": "x", "type": "SPACER"}}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.status") { value(400) }
            jsonPath("$.message") { value(containsString("Malformed JSON request body")) }
        }
    }

    @Test
    fun `unknown JSON properties are ignored`() {
        given(screenService.upsert(eqArg("home"), anyArg<ScreenDto>())).willReturn(seed)

        mockMvc.put("/api/v1/screens/home") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"futureField": 1, "root": {"id": "r", "type": "COLUMN", "elevation": 4, "children": null}}"""
        }.andExpect {
            status { isOk() }
        }
    }

    /** Mockito matchers return null; these wrappers keep Kotlin from inserting a call-site null check. */
    @Suppress("UNCHECKED_CAST")
    private fun <T> anyArg(): T = ArgumentMatchers.any<T>() as T

    private fun <T> eqArg(value: T): T = ArgumentMatchers.eq(value) ?: value
}
