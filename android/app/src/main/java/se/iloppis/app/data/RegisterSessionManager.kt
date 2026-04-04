package se.iloppis.app.data

import android.content.Context
import androidx.core.content.edit
import se.iloppis.app.network.cashier.RegisterLifecycleEventType
import java.util.UUID

/**
 * ILP-003-08: Register session lifecycle manager for Android cashier.
 *
 * Persists session state in SharedPreferences so it survives process death and restarts.
 * State machine mirrors the backend/desktop contract:
 *
 * ```
 * OPEN ──► CLOSE_REQUESTED ──► CLOSED
 *   └─────────────────────────────► FORCED_CLOSED
 * ```
 *
 * CLOSED and FORCED_CLOSED are terminal; call [openSession] to start a fresh session.
 *
 * Thread-safety: all public methods are [Synchronized] via the companion object lock.
 */
class RegisterSessionManager private constructor(private val appContext: Context) {

    enum class State { OPEN, CLOSE_REQUESTED, CLOSED, FORCED_CLOSED }

    data class Session(
        val sessionId: String,
        val eventId: String,
        val registerId: String,
        val state: State,
        /** The lifecycle event type that should be sent on the NEXT heartbeat tick after a transition. */
        val pendingLifecycleEvent: RegisterLifecycleEventType?
    )

    private val prefs by lazy {
        appContext.getSharedPreferences("register_session", Context.MODE_PRIVATE)
    }

    private var current: Session? = null

    init {
        // Restore persisted state on first access.
        current = loadFromPrefs()
    }

    // ─────────────────────────────────────────────────────────── lifecycle API

    @Synchronized
    fun openSession(eventId: String, registerId: String): Session {
        val s = Session(
            sessionId = UUID.randomUUID().toString(),
            eventId = eventId,
            registerId = registerId,
            state = State.OPEN,
            pendingLifecycleEvent = RegisterLifecycleEventType.REGISTER_LIFECYCLE_OPEN
        )
        current = s
        persist(s)
        return s
    }

    @Synchronized
    fun requestClose(): Boolean {
        val s = current ?: return false
        if (s.state != State.OPEN) return false
        val updated = s.copy(
            state = State.CLOSE_REQUESTED,
            pendingLifecycleEvent = RegisterLifecycleEventType.REGISTER_LIFECYCLE_CLOSE_REQUESTED
        )
        current = updated
        persist(updated)
        return true
    }

    @Synchronized
    fun confirmClose(): Boolean {
        val s = current ?: return false
        if (s.state != State.CLOSE_REQUESTED) return false
        val updated = s.copy(
            state = State.CLOSED,
            pendingLifecycleEvent = RegisterLifecycleEventType.REGISTER_LIFECYCLE_CLOSE_CONFIRMED
        )
        current = updated
        persist(updated)
        return true
    }

    /**
     * Called after a lifecycle event has been successfully sent in a heartbeat tick.
     *
     * Clears only when the in-flight request matches the current session + pending
     * lifecycle event to avoid dropping a newer transition set while the request was
     * in flight.
     */
    @Synchronized
    fun clearPendingLifecycleEvent(
        expectedLifecycleEvent: RegisterLifecycleEventType?,
        expectedSessionId: String?
    ) {
        val s = current ?: return
        if (expectedLifecycleEvent == null || expectedSessionId.isNullOrBlank()) return
        if (s.sessionId != expectedSessionId) return
        if (s.pendingLifecycleEvent != expectedLifecycleEvent) return
        current = s.copy(pendingLifecycleEvent = null)
        persist(current!!)
    }

    @Synchronized
    fun recordSync() {
        val s = current ?: return
        if (s.state == State.CLOSED || s.state == State.FORCED_CLOSED) return
        val updated = s.copy(
            pendingLifecycleEvent = RegisterLifecycleEventType.REGISTER_LIFECYCLE_SYNC
        )
        current = updated
        persist(updated)
    }

    // ─────────────────────────────────────────────────────────── queries

    @Synchronized
    fun getCurrent(): Session? = current

    @Synchronized
    fun isSessionActive(): Boolean =
        current?.state == State.OPEN || current?.state == State.CLOSE_REQUESTED

    // ─────────────────────────────────────────────────────────── persistence

    private fun persist(s: Session) {
        prefs.edit {
            putString("session_id", s.sessionId)
            putString("event_id", s.eventId)
            putString("register_id", s.registerId)
            putString("state", s.state.name)
            putString("pending_lifecycle", s.pendingLifecycleEvent?.name)
        }
    }

    private fun loadFromPrefs(): Session? {
        val sessionId = prefs.getString("session_id", null) ?: return null
        val eventId = prefs.getString("event_id", null) ?: return null
        val registerId = prefs.getString("register_id", null) ?: return null
        val state = prefs.getString("state", null)?.let {
            runCatching { State.valueOf(it) }.getOrNull()
        } ?: return null
        val pending = prefs.getString("pending_lifecycle", null)?.let {
            runCatching { RegisterLifecycleEventType.valueOf(it) }.getOrNull()
        }
        return Session(sessionId, eventId, registerId, state, pending)
    }

    companion object {
        @Volatile
        private var instance: RegisterSessionManager? = null

        fun getInstance(context: Context): RegisterSessionManager =
            instance ?: synchronized(this) {
                instance ?: RegisterSessionManager(context.applicationContext).also { instance = it }
            }
    }
}
