package com.ssui.mobile.ui.screen

import com.ssui.mobile.domain.Action
import com.ssui.mobile.domain.Screen

/** MVI contract, verbatim from spec 6.4. */
sealed interface ScreenUiState {
    data object Loading : ScreenUiState
    data class Success(val screen: Screen) : ScreenUiState
    data class Error(val message: String) : ScreenUiState
}

sealed interface ScreenEvent {
    data object Load : ScreenEvent
    data object Retry : ScreenEvent
    /** User asked to re-fetch the screen from the server (refresh icon in the top bar). */
    data object Refresh : ScreenEvent
    data class ElementClicked(val elementId: String, val action: Action) : ScreenEvent
}

/** One-shot side effects, emitted through a Channel and performed by the composable. */
sealed interface ScreenEffect {
    data class ShowToast(val message: String) : ScreenEffect
    data class OpenUrl(val url: String) : ScreenEffect
}
