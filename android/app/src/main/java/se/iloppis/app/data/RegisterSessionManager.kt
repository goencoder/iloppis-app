package se.iloppis.app.data

import android.content.Context
import androidx.core.content.edit
import se.iloppis.app.network.cashier.RegisterLifecycleEventType
import java.util.UUID

/**
 * Persists session state in SharedPreferences so it survives process death and restarts.
 * State machine mirrors the backend/desktop contract:
 *
 * ```
 * OPEN ──► CLOSE_REQUESTED ──► CLOSED
 * ```
 *
 * CLOSED is terminal; call [openSession] to start a fresh session.
 *
 * Thread-safety: public methods annotated with [Synchronized] are synchronized on this
 * [RegisterSessionManager] instance monitor.
 */
class RegisterSessionManager private constructor(private val appContext: Context) {

    enum class State { OPEN, CLOSE_REQUESTED, CLOSED }

    data class Session(
        val sessionId: String,
        val eventId: String,
        val registerId: String,
        val state: State,
        /**
         * The lifecycle event type that should be sent on the NEXT heartbeat tick after a
         * transition. When no transition is pending, [recordSync] can set SYNC to report
         * liveness once per successful heartbeat cycle.
         */
        val pendingLifecycleEvent: RegisterLifecycleEventType?
    )

    private val prefs by lazy {
        appContext.getSharedPreferences("register_session", Context.MODE_PRIVATE)
    }

    private var current: Session? = null

    init {
        current = loadFromPrefs()
    }

    /**
     * Replaces any current session with a new open session for the given event and register.
     * The returned session has a fresh ID and a pending `OPEN` lifecycle event.
     */
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

    /**
     * Moves an open session to `CLOSE_REQUESTED` and schedules that lifecycle event.
     *
     * @return `true` only when the state transition was applied.
     */
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

    /**
     * Schedules `CLOSE_CONFIRMED` for a session awaiting closure.
     * The session becomes closed only after [clearPendingLifecycleEvent] acknowledges it.
     *
     * @return `true` only when confirmation was scheduled.
     */
    @Synchronized
    fun confirmClose(): Boolean {
        val s = current ?: return false
        if (s.state != State.CLOSE_REQUESTED) return false
        val updated = s.copy(
            pendingLifecycleEvent = RegisterLifecycleEventType.REGISTER_LIFECYCLE_CLOSE_CONFIRMED
        )
        current = updated
        persist(updated)
        return true
    }

    /**
     * Clears a delivered lifecycle event only when both expected values still match.
     * This prevents a stale response from discarding a newer session transition.
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
        val nextState = if (
            expectedLifecycleEvent == RegisterLifecycleEventType.REGISTER_LIFECYCLE_CLOSE_CONFIRMED &&
            s.state == State.CLOSE_REQUESTED
        ) {
            State.CLOSED
        } else {
            s.state
        }
        current = s.copy(state = nextState, pendingLifecycleEvent = null)
        persist(current!!)
    }

    /** Schedules a liveness sync unless a transition is pending or the session is closed. */
    @Synchronized
    fun recordSync() {
        val s = current ?: return
        if (s.state == State.CLOSED) return
        if (s.pendingLifecycleEvent != null) return
        val updated = s.copy(
            pendingLifecycleEvent = RegisterLifecycleEventType.REGISTER_LIFECYCLE_SYNC
        )
        current = updated
        persist(updated)
    }

    /** Returns the current persisted session snapshot, or `null` before the first session. */
    @Synchronized
    fun getCurrent(): Session? = current

    /** Returns whether the current session can still send cashier activity. */
    @Synchronized
    fun isSessionActive(): Boolean =
        current?.state == State.OPEN || current?.state == State.CLOSE_REQUESTED

    private fun persist(s: Session) {
        val commitNow = s.state == State.CLOSED
        prefs.edit(commit = commitNow) {
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

        /** Returns the process-wide manager backed by the application context. */
        fun getInstance(context: Context): RegisterSessionManager =
            instance ?: synchronized(this) {
                instance ?: RegisterSessionManager(context.applicationContext).also { instance = it }
            }
    }
}
