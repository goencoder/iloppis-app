package se.iloppis.app.domain.model

import se.iloppis.app.R
import se.iloppis.app.network.events.EventLifecycle
import java.time.Instant

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

    /** Raw start time for computed display status */
    val startTime: Instant? = null,
    /** Raw end time for computed display status */
    val endTime: Instant? = null,
    /** Backend lifecycle state (OPEN, CLOSED, FINALIZED, etc.) */
    val lifecycleState: EventLifecycle? = null,
    /** Street address */
    val addressStreet: String? = null,
    /** City */
    val addressCity: String? = null,
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
 * Computed display status for events.
 *
 * Unlike [EventState] which maps backend lifecycle, this is computed
 * client-side from time ranges: startTime <= now <= endTime → ONGOING.
 */
enum class EventDisplayStatus(val stringResId: Int) {
    ONGOING(R.string.filter_ongoing),    // Pågående
    UPCOMING(R.string.filter_upcoming),  // Kommande
    PAST(R.string.filter_past),          // Förflutna
}

/**
 * Compute display status from raw time fields.
 * "Pågående" is computed, not a lifecycle state.
 */
fun Event.displayStatus(): EventDisplayStatus {
    val now = Instant.now()
    return when {
        lifecycleState in listOf(
            EventLifecycle.CLOSED,
            EventLifecycle.LIFECYCLE_STATE_CLOSED,
            EventLifecycle.FINALIZED
        ) -> EventDisplayStatus.PAST

        startTime != null && endTime != null
            && !now.isBefore(startTime) && !now.isAfter(endTime) -> EventDisplayStatus.ONGOING

        startTime != null && now.isBefore(startTime) -> EventDisplayStatus.UPCOMING

        else -> EventDisplayStatus.PAST
    }
}

/**
 * Mode for code entry - determines which type of code is being entered.
 */
enum class CodeEntryMode {
    TOOL,
    CASHIER,
    SCANNER,
    LIVE_STATS
}
