package se.iloppis.app.ui.util

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/** Text supplied either by an Android resource or at runtime. */
sealed class UiText {

    /** A string backed by an Android string resource, optionally with format args. */
    data class StringResource(
        @param:StringRes val resId: Int,
        val args: List<Any> = emptyList()
    ) : UiText()

    /** A plain runtime string (e.g. from a server response). */
    data class DynamicString(val value: String) : UiText()

    // ── Composable resolver ──────────────────────────────────────────

    /** Resolves this value using the current Compose resources and locale. */
    @Composable
    fun asString(): String = when (this) {
        is StringResource -> stringResource(resId, *args.toTypedArray())
        is DynamicString -> value
    }

    // ── Non-composable resolver (for tests, services, etc.) ─────────

    /** Resolves this value using [context]'s resources and locale. */
    fun asString(context: Context): String = when (this) {
        is StringResource -> context.getString(resId, *args.toTypedArray())
        is DynamicString -> value
    }
}
