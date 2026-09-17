package com.ssui.mobile.ui.screen

import com.ssui.mobile.domain.Screen
import com.ssui.mobile.domain.ScreenRepository
import kotlinx.coroutines.CompletableDeferred

/** Scriptable repository: queue results (a Screen or a Throwable) and optionally hold the call open. */
class FakeScreenRepository : ScreenRepository {
    private val results = ArrayDeque<Result<Screen>>()
    val requestedNames = mutableListOf<String>()
    var gate: CompletableDeferred<Unit>? = null

    fun enqueueSuccess(screen: Screen) = results.addLast(Result.success(screen))
    fun enqueueFailure(error: Throwable) = results.addLast(Result.failure(error))

    override suspend fun getScreen(name: String): Screen {
        requestedNames += name
        gate?.await()
        val next = results.removeFirstOrNull() ?: error("FakeScreenRepository: no result enqueued for '$name'")
        return next.getOrThrow()
    }
}
