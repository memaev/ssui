package com.ssui.backend.seed

import com.ssui.backend.TestJson
import com.ssui.backend.api.ScreenValidator
import com.ssui.backend.mapping.ScreenMapper
import com.ssui.backend.persistence.ScreenEntity
import com.ssui.backend.persistence.ScreenRepository
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import kotlin.test.assertEquals

class DataSeederTest {

    private val repository: ScreenRepository = mock(ScreenRepository::class.java)
    private val seeder = DataSeeder(repository, ScreenMapper(), ScreenValidator(), SeedScreenReader(TestJson.objectMapper))

    @Test
    fun `inserts home from the seed file when it does not exist`() {
        given(repository.existsByName("home")).willReturn(false)
        given(repository.insert(any(ScreenEntity::class.java))).willAnswer { it.getArgument<ScreenEntity>(0) }

        seeder.run()

        val captor = ArgumentCaptor.forClass(ScreenEntity::class.java)
        then(repository).should().insert(captor.capture())
        assertEquals("3f0c1b2e-9a1d-4a4c-8f1e-1e2d3c4b5a60", captor.value.id)
        assertEquals("home", captor.value.name)
        assertEquals("Tap me", captor.value.root.children[3].textContent)
    }

    @Test
    fun `never overwrites an existing home screen`() {
        given(repository.existsByName("home")).willReturn(true)

        seeder.run()

        then(repository).should(never()).insert(any(ScreenEntity::class.java))
    }
}
