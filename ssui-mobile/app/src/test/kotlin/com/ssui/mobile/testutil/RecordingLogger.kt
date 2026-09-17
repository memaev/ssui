package com.ssui.mobile.testutil

import com.ssui.mobile.domain.Logger

/** Collects log lines so tests can assert that dropped elements / ignored actions are logged. */
class RecordingLogger : Logger {
    data class Entry(val level: Char, val tag: String, val message: String, val throwable: Throwable? = null)

    val entries = mutableListOf<Entry>()

    val warnings: List<String> get() = entries.filter { it.level == 'W' }.map { it.message }
    val errors: List<String> get() = entries.filter { it.level == 'E' }.map { it.message }
    val infos: List<String> get() = entries.filter { it.level == 'I' }.map { it.message }

    override fun d(tag: String, message: String) { entries += Entry('D', tag, message) }
    override fun i(tag: String, message: String) { entries += Entry('I', tag, message) }
    override fun w(tag: String, message: String) { entries += Entry('W', tag, message) }
    override fun e(tag: String, message: String, throwable: Throwable?) { entries += Entry('E', tag, message, throwable) }
}
