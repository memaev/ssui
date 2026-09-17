package com.ssui.mobile.domain

interface ScreenRepository {
    /**
     * Loads and validates the screen with the given [name].
     * Throws on network/HTTP/parsing failure; the caller turns that into an error state.
     */
    suspend fun getScreen(name: String): Screen
}
