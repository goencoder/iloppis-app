package se.iloppis.app.ui.utils

import java.time.Instant
import java.time.format.DateTimeFormatter

fun formatValidTimeOrUnknown(
    value: Instant?,
    fallback: String,
    formatter: DateTimeFormatter
): String {
    return value?.let { formatter.format(it) } ?: fallback
}
