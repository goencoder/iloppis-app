package se.iloppis.app.data.mappers

import se.iloppis.app.domain.model.Event
import se.iloppis.app.domain.model.EventState
import se.iloppis.app.network.events.ApiEvent
import se.iloppis.app.network.events.EventLifecycle
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Maps API DTOs to domain models.
 */
object EventMapper {
    private val svLocale = Locale.Builder().setLanguage("sv").setRegion("SE").build()
    private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", svLocale)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", svLocale)

    /**
     * Converts API Event object to Event object
     */
    fun ApiEvent.toDomain() : Event {
        val startParsed = startTime?.let { runCatching { ZonedDateTime.parse(it) }.getOrNull() }
        val endParsed = endTime?.let { runCatching { ZonedDateTime.parse(it) }.getOrNull() }

        val dates = formatDateRange(startParsed, endParsed)
        val startTimeStr = startParsed?.format(timeFormatter) ?: ""
        val endTimeStr = endParsed?.format(timeFormatter) ?: ""
        val location = formatLocation(addressStreet, addressCity)

        return Event(
            id = id,
            name = name,
            description = description ?: "",
            dates = dates,
            startTimeFormatted = startTimeStr,
            endTimeFormatted = endTimeStr,
            location = location,
            state = lifecycleState?.toEventState() ?: EventState.UNKNOWN,
            latitude = latitude,
            longitude = longitude,
        )
    }



    private fun mapEventState(apiState: String?): EventState = when (apiState) {
        "OPEN" -> EventState.OPEN
        "LIFECYCLE_STATE_OPEN" -> EventState.OPEN  // Legacy format
        "CLOSED" -> EventState.CLOSED
        "LIFECYCLE_STATE_CLOSED" -> EventState.CLOSED  // Legacy format
        "FINALIZED" -> EventState.CLOSED  // Treat finalized as closed
        "PENDING" -> EventState.UPCOMING
        "LIFECYCLE_STATE_PENDING" -> EventState.UPCOMING  // Legacy format
        else -> EventState.UNKNOWN
    }


    /**
     * Converts [EventLifecycle] to [EventState]
     */
    private fun EventLifecycle.toEventState() : EventState = when(this) {
        EventLifecycle.OPEN -> EventState.OPEN
        EventLifecycle.LIFECYCLE_STATE_OPEN -> EventState.OPEN

        EventLifecycle.CLOSED -> EventState.CLOSED
        EventLifecycle.LIFECYCLE_STATE_CLOSED -> EventState.CLOSED

        EventLifecycle.FINALIZED -> EventState.CLOSED

        EventLifecycle.PENDING -> EventState.UPCOMING
        EventLifecycle.LIFECYCLE_STATE_PENDING -> EventState.UPCOMING
    }


    private fun formatDateRange(start: ZonedDateTime?, end: ZonedDateTime?): String = when {
        start != null && end != null && start.toLocalDate() == end.toLocalDate() ->
            start.format(dateFormatter)
        start != null && end != null ->
            "${start.format(dateFormatter)} – ${end.format(dateFormatter)}"
        start != null -> start.format(dateFormatter)
        else -> ""
    }

    private fun formatLocation(street: String?, city: String?): String =
        listOfNotNull(
            street?.takeIf { it.isNotBlank() },
            city?.takeIf { it.isNotBlank() }
        ).joinToString(", ")
}
