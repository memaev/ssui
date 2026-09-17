package com.ssui.mobile.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssui.mobile.domain.Action
import com.ssui.mobile.domain.ActionType
import com.ssui.mobile.domain.Logger
import com.ssui.mobile.domain.ScreenRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Single immutable [ScreenUiState] + single [onEvent] entry point (spec 6.1 / 6.4).
 * No Android framework types in here so it runs in plain JVM unit tests.
 */
class ScreenViewModel(
    private val repository: ScreenRepository,
    private val logger: Logger = Logger.None,
    private val screenName: String = DEFAULT_SCREEN_NAME,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScreenUiState>(ScreenUiState.Loading)
    val uiState: StateFlow<ScreenUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ScreenEffect>(Channel.BUFFERED)
    val effects: Flow<ScreenEffect> = _effects.receiveAsFlow()

    private var loadJob: Job? = null

    fun onEvent(event: ScreenEvent) {
        when (event) {
            // Load is idempotent: recomposition or Activity re-creation must not refetch.
            ScreenEvent.Load -> if (loadJob == null) load()
            ScreenEvent.Retry -> load()
            ScreenEvent.Refresh -> load()
            is ScreenEvent.ElementClicked -> handleClick(event)
        }
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = ScreenUiState.Loading
            try {
                val screen = repository.getScreen(screenName)
                _uiState.value = ScreenUiState.Success(screen)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e(TAG, "Failed to load screen '$screenName'", e)
                _uiState.value = ScreenUiState.Error(e.toUserMessage())
            }
        }
    }

    private fun handleClick(event: ScreenEvent.ElementClicked) {
        val action = event.action
        logger.i(TAG, "Element ${event.elementId} clicked: $action")
        val effect: ScreenEffect? = when (action.type) {
            ActionType.SHOW_TOAST -> action.requirePayload("message")?.let(ScreenEffect::ShowToast)
            ActionType.OPEN_URL -> action.requirePayload("url")?.let(ScreenEffect::OpenUrl)
            ActionType.NAVIGATE -> {
                logger.i(TAG, "NAVIGATE to '${action.payload["screen"]}' is not implemented in the POC")
                null
            }
        }
        if (effect != null) {
            viewModelScope.launch { _effects.send(effect) }
        }
    }

    private fun Action.requirePayload(key: String): String? {
        val value = payload[key]
        if (value.isNullOrBlank()) {
            logger.w(TAG, "Action $type is missing payload key '$key'; ignored")
            return null
        }
        return value
    }

    private fun Exception.toUserMessage(): String {
        val detail = message?.takeIf { it.isNotBlank() } ?: (this::class.simpleName ?: "Unknown error")
        return "Could not load screen '$screenName': $detail"
    }

    companion object {
        const val DEFAULT_SCREEN_NAME = "home"
        private const val TAG = "ScreenViewModel"
    }
}
