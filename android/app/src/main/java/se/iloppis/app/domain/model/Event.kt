package se.iloppis.app.domain.model

import se.iloppis.app.R

/**
 * Domain model representing an event (loppis).
 */
data class Event(
    val id: String,
    val name: String,
    val description: String,
    val dates: String,
    val startTimeFormatted: String,
    val endTimeFormatted: String,
    val location: String,
    val state: EventState,

    val latitude: Double?,
    val longitude: Double?,
)

/**
 * Possible states for an event.
 * Use stringResId with stringResource() to get localized display name.
 */
enum class EventState(val stringResId: Int) {
    OPEN(R.string.state_open),
    UPCOMING(R.string.state_upcoming),
    CLOSED(R.string.state_closed),
    UNKNOWN(R.string.state_closed)  // Fallback to closed
}

/**
 * Mode for code entry - determines which type of code is being entered.
 */
enum class CodeEntryMode {
    CASHIER,
    SCANNER
}
