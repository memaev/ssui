package com.ssui.mobile.data.remote

import com.ssui.mobile.data.remote.dto.DtoParseIssueReporter
import com.ssui.mobile.data.remote.dto.ScreenDto
import com.ssui.mobile.testutil.SEED_HOME_JSON
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UiElementDtoSerializationTest {

    private val issues = mutableListOf<String>()

    @Before
    fun setUp() {
        DtoParseIssueReporter.sink = { issues += it }
    }

    @After
    fun tearDown() {
        DtoParseIssueReporter.sink = null
    }

    @Test
    fun `parses the seed document`() {
        val dto = SsuiJson.decodeFromString(ScreenDto.serializer(), SEED_HOME_JSON)

        assertEquals("home", dto.name)
        assertEquals("COLUMN", dto.root.type)
        assertEquals(4, dto.root.children.size)
        assertEquals("SHOW_TOAST", dto.root.children[3].onClick?.type)
        assertTrue(issues.isEmpty())
    }

    @Test
    fun `unknown JSON fields are ignored`() {
        val json = """{"id":"s","name":"home","futureField":1,"root":{"id":"r","type":"COLUMN","elevation":4}}"""

        val dto = SsuiJson.decodeFromString(ScreenDto.serializer(), json)

        assertEquals("r", dto.root.id)
        assertTrue(dto.root.children.isEmpty())
    }

    @Test
    fun `a child whose JSON is malformed is dropped and reported, siblings survive`() {
        val json = """
            {"id":"s","name":"home","root":{"id":"r","type":"COLUMN","children":[
              {"id":"ok-1","type":"TEXT","textContent":"a"},
              {"id":"broken","type":"TEXT","textContent":"b","padding":{"start":"lots","top":0,"end":0,"bottom":0}},
              {"id":"no-type","textContent":"c"},
              {"id":"ok-2","type":"TEXT","textContent":"d"}
            ]}}
        """.trimIndent()

        val dto = SsuiJson.decodeFromString(ScreenDto.serializer(), json)

        assertEquals(listOf("ok-1", "ok-2"), dto.root.children.map { it.id })
        assertEquals(2, issues.size)
        assertTrue(issues[0].contains("broken"))
        assertTrue(issues[1].contains("no-type"))
    }
}
