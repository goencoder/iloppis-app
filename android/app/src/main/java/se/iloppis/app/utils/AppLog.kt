package se.iloppis.app.utils

import se.iloppis.app.BuildConfig

/**
 * Centralizes app-internal logging so production builds can stay quiet.
 */
object AppLog {
    private val isEnabled: Boolean
        get() = BuildConfig.DEBUG || BuildConfig.ENABLE_NETWORK_DEBUG_LOGGING

    fun d(tag: String, message: String) {
        if (isEnabled) android.util.Log.d(tag, message)
    }

    fun i(tag: String, message: String) {
        if (isEnabled) android.util.Log.i(tag, message)
    }

    fun w(tag: String, message: String) {
        if (isEnabled) android.util.Log.w(tag, message)
    }

    fun w(tag: String, message: String, throwable: Throwable) {
        if (isEnabled) android.util.Log.w(tag, message, throwable)
    }

    fun e(tag: String, message: String) {
        if (isEnabled) android.util.Log.e(tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable) {
        if (isEnabled) android.util.Log.e(tag, message, throwable)
    }
}