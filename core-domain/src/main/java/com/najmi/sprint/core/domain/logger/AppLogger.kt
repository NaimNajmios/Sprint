package com.najmi.sprint.core.domain.logger

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

enum class LogLevel { DEBUG, INFO, WARN, ERROR }

data class LogEntry(
    val timestamp: Instant,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val exception: Throwable? = null
)

/**
 * In-memory ring buffer logger that forwards to Logcat and exposes a StateFlow
 * for the in-app Debug Console.
 */
object AppLogger {
    private const val MAX_LOGS = 500
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun d(tag: String, message: String) {
        Log.d(tag, message)
        addLog(LogLevel.DEBUG, tag, message)
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        addLog(LogLevel.INFO, tag, message)
    }

    fun w(tag: String, message: String, e: Throwable? = null) {
        Log.w(tag, message, e)
        addLog(LogLevel.WARN, tag, message, e)
    }

    fun e(tag: String, message: String, e: Throwable? = null) {
        Log.e(tag, message, e)
        addLog(LogLevel.ERROR, tag, message, e)
    }

    private fun addLog(level: LogLevel, tag: String, message: String, e: Throwable? = null) {
        val entry = LogEntry(Clock.System.now(), level, tag, message, e)
        val currentList = _logs.value.toMutableList()
        currentList.add(0, entry) // Add to top so latest is first
        if (currentList.size > MAX_LOGS) {
            currentList.removeAt(currentList.lastIndex)
        }
        _logs.value = currentList
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
