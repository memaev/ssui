package com.ssui.backend.domain

import com.ssui.backend.TestJson
import com.ssui.backend.mapping.ScreenMapper
import com.ssui.backend.persistence.ScreenEntity
import com.ssui.backend.persistence.ScreenRepository
import com.ssui.backend.seed.SeedScreenReader
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mockito.mock
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ScreenServiceTest {

    private val repository: ScreenRepository = mock(ScreenRepository::class.java)
    private val mapper = ScreenMapper()
    private val service = ScreenService(repository, mapper)
    private val seed = SeedScreenReader(TestJson.objectMapper).read()

    init {
        given(repository.save(any(ScreenEntity::class.java))).willAnswer { it.getArgument<ScreenEntity>(0) }
    }

    @Test
    fun `getByName maps the entity to a dto`() {
        given(repository.findByName("home")).willReturn(mapper.toEntity(seed))

        assertEquals(seed, service.getByName("home"))
    }

    @Test
    fun `getByName throws when the screen does not exist`() {
        given(repository.findByName("nope")).willReturn(null)

        val ex = assertThrows<ScreenNotFoundException> { service.getByName("nope") }
        assertEquals("Screen 'nope' not found", ex.message)
    }

    @Test
    fun `upsert keeps the existing id and uses the path name when replacing`() {
        given(repository.findByName("home")).willReturn(mapper.toEntity(seed))
        val body = seed.copy(id = "client-supplied", name = "somethingElse")

        val result = service.upsert("home", body)

        val captor = ArgumentCaptor.forClass(ScreenEntity::class.java)
        then(repository).should().save(captor.capture())
        assertEquals(seed.id, captor.value.id)
        assertEquals("home", captor.value.name)
        assertEquals(seed.id, result.id)
        assertEquals("home", result.name)
    }

    @Test
    fun `upsert generates a uuid when creating without an id`() {
        given(repository.findByName("new")).willReturn(null)

        val result = service.upsert("new", seed.copy(id = null, name = null))

        assertEquals("new", result.name)
        UUID.fromString(result.id) // valid UUID
        assertNotEquals(seed.id, result.id)
    }

    @Test
    fun `upsert honours a client-supplied id when creating`() {
        given(repository.findByName("new")).willReturn(null)

        val result = service.upsert("new", seed.copy(id = "my-id"))

        assertEquals("my-id", result.id)
    }
}
