package se.iloppis.app.utils

import se.iloppis.app.BuildConfig

/**
 * Centralizes app-internal logging so production builds can stay quiet.
 */
object AppLog {
    private val isEnabled: Boolean
        get() = BuildConfig.DEBUG || BuildConfig.ENABLE_NETWORK_DEBUG_LOGGING

    /** Writes a debug message when logging is enabled for this build. */
    fun d(tag: String, message: String) {
        if (isEnabled) android.util.Log.d(tag, message)
    }

    /** Writes an informational message when logging is enabled for this build. */
    fun i(tag: String, message: String) {
        if (isEnabled) android.util.Log.i(tag, message)
    }

    /** Writes a warning when logging is enabled for this build. */
    fun w(tag: String, message: String) {
        if (isEnabled) android.util.Log.w(tag, message)
    }

    /** Writes a warning and its cause when logging is enabled for this build. */
    fun w(tag: String, message: String, throwable: Throwable) {
        if (isEnabled) android.util.Log.w(tag, message, throwable)
    }

    /** Writes an error when logging is enabled for this build. */
    fun e(tag: String, message: String) {
        if (isEnabled) android.util.Log.e(tag, message)
    }

    /** Writes an error and its cause when logging is enabled for this build. */
    fun e(tag: String, message: String, throwable: Throwable) {
        if (isEnabled) android.util.Log.e(tag, message, throwable)
    }
}
