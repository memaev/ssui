package com.ssui.mobile.ui.screen

import app.cash.turbine.test
import com.ssui.mobile.domain.Action
import com.ssui.mobile.domain.ActionType
import com.ssui.mobile.domain.ElementType
import com.ssui.mobile.domain.Screen
import com.ssui.mobile.domain.Size
import com.ssui.mobile.domain.UiElement
import com.ssui.mobile.testutil.RecordingLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeScreenRepository()
    private val logger = RecordingLogger()

    private val homeScreen = Screen(
        id = "screen-1",
        name = "home",
        root = UiElement(id = "root", type = ElementType.COLUMN, width = Size.Fill, height = Size.Wrap),
    )

    private fun viewModel() = ScreenViewModel(repository = repository, logger = logger)

    @Test
    fun `Load goes from Loading to Success`() = runTest {
        repository.enqueueSuccess(homeScreen)
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(ScreenUiState.Loading, awaitItem())
            viewModel.onEvent(ScreenEvent.Load)
            assertEquals(ScreenUiState.Success(homeScreen), awaitItem())
        }
        assertEquals(listOf("home"), repository.requestedNames)
    }

    @Test
    fun `Load stays in Loading while the request is in flight`() = runTest {
        val gate = CompletableDeferred<Unit>()
        repository.gate = gate
        repository.enqueueSuccess(homeScreen)
        val viewModel = viewModel()

        viewModel.onEvent(ScreenEvent.Load)
        advanceUntilIdle()
        assertEquals(ScreenUiState.Loading, viewModel.uiState.value)

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(ScreenUiState.Success(homeScreen), viewModel.uiState.value)
    }

    @Test
    fun `Load goes from Loading to Error with the failure message`() = runTest {
        repository.enqueueFailure(IOException("Failed to connect to /10.0.2.2:8080"))
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(ScreenUiState.Loading, awaitItem())
            viewModel.onEvent(ScreenEvent.Load)
            val error = awaitItem()
            assertTrue(error is ScreenUiState.Error)
            assertTrue((error as ScreenUiState.Error).message.contains("Failed to connect to /10.0.2.2:8080"))
        }
        assertEquals(1, logger.errors.size)
    }

    @Test
    fun `Load is sent only once even if the event is repeated`() = runTest {
        repository.enqueueSuccess(homeScreen)
        val viewModel = viewModel()

        viewModel.onEvent(ScreenEvent.Load)
        advanceUntilIdle()
        viewModel.onEvent(ScreenEvent.Load) // e.g. after rotation
        viewModel.onEvent(ScreenEvent.Load)
        advanceUntilIdle()

        assertEquals(1, repository.requestedNames.size)
        assertEquals(ScreenUiState.Success(homeScreen), viewModel.uiState.value)
    }

    @Test
    fun `Retry after an error reloads and reaches Success`() = runTest {
        repository.enqueueFailure(IOException("connection refused"))
        repository.enqueueSuccess(homeScreen)
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(ScreenUiState.Loading, awaitItem())
            viewModel.onEvent(ScreenEvent.Load)
            assertTrue(awaitItem() is ScreenUiState.Error)

            viewModel.onEvent(ScreenEvent.Retry)
            assertEquals(ScreenUiState.Loading, awaitItem())
            assertEquals(ScreenUiState.Success(homeScreen), awaitItem())
        }
        assertEquals(2, repository.requestedNames.size)
    }

    @Test
    fun `Refresh re-fetches the screen and replaces the previous Success`() = runTest {
        val updatedScreen = homeScreen.copy(id = "screen-2")
        repository.enqueueSuccess(homeScreen)
        repository.enqueueSuccess(updatedScreen)
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(ScreenUiState.Loading, awaitItem())
            viewModel.onEvent(ScreenEvent.Load)
            assertEquals(ScreenUiState.Success(homeScreen), awaitItem())

            viewModel.onEvent(ScreenEvent.Refresh)
            assertEquals(ScreenUiState.Loading, awaitItem())
            assertEquals(ScreenUiState.Success(updatedScreen), awaitItem())
        }
        assertEquals(listOf("home", "home"), repository.requestedNames)
    }

    @Test
    fun `Refresh failure moves to Error even after a previous Success`() = runTest {
        repository.enqueueSuccess(homeScreen)
        repository.enqueueFailure(IOException("connection refused"))
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(ScreenUiState.Loading, awaitItem())
            viewModel.onEvent(ScreenEvent.Load)
            assertEquals(ScreenUiState.Success(homeScreen), awaitItem())

            viewModel.onEvent(ScreenEvent.Refresh)
            assertEquals(ScreenUiState.Loading, awaitItem())
            assertTrue(awaitItem() is ScreenUiState.Error)
        }
    }

    @Test
    fun `ElementClicked with SHOW_TOAST emits ShowToast effect`() = runTest {
        val viewModel = viewModel()
        val action = Action(ActionType.SHOW_TOAST, mapOf("message" to "Hello from the server!"))

        viewModel.effects.test {
            viewModel.onEvent(ScreenEvent.ElementClicked("btn-1", action))
            assertEquals(ScreenEffect.ShowToast("Hello from the server!"), awaitItem())
            expectNoEvents()
        }
        assertTrue(logger.infos.any { it.contains("btn-1") })
    }

    @Test
    fun `ElementClicked with OPEN_URL emits OpenUrl effect`() = runTest {
        val viewModel = viewModel()
        val action = Action(ActionType.OPEN_URL, mapOf("url" to "https://example.com"))

        viewModel.effects.test {
            viewModel.onEvent(ScreenEvent.ElementClicked("img-1", action))
            assertEquals(ScreenEffect.OpenUrl("https://example.com"), awaitItem())
        }
    }

    @Test
    fun `NAVIGATE is parsed but only logged in the POC`() = runTest {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onEvent(ScreenEvent.ElementClicked("btn-2", Action(ActionType.NAVIGATE, mapOf("screen" to "details"))))
            advanceUntilIdle()
            expectNoEvents()
        }
        assertTrue(logger.infos.any { it.contains("NAVIGATE") && it.contains("details") })
    }

    @Test
    fun `SHOW_TOAST without a message is ignored and logged`() = runTest {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onEvent(ScreenEvent.ElementClicked("btn-3", Action(ActionType.SHOW_TOAST, emptyMap())))
            advanceUntilIdle()
            expectNoEvents()
        }
        assertTrue(logger.warnings.any { it.contains("message") })
    }
}
