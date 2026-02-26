package se.iloppis.app.ui.util

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * Wrapper for text that can originate from string resources (with optional args)
 * or from dynamic/server-supplied strings.
 *
 * ViewModels produce [UiText] values; Composables call [asString] to resolve them.
 */
sealed class UiText {

    /** A string backed by an Android string resource, optionally with format args. */
    data class StringResource(
        @param:StringRes val resId: Int,
        val args: List<Any> = emptyList()
    ) : UiText()

    /** A plain runtime string (e.g. from a server response). */
    data class DynamicString(val value: String) : UiText()

    // ── Composable resolver ──────────────────────────────────────────

    @Composable
    fun asString(): String = when (this) {
        is StringResource -> stringResource(resId, *args.toTypedArray())
        is DynamicString -> value
    }

    // ── Non-composable resolver (for tests, services, etc.) ─────────

    fun asString(context: Context): String = when (this) {
        is StringResource -> context.getString(resId, *args.toTypedArray())
        is DynamicString -> value
    }
}
