package se.iloppis.app.data

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import se.iloppis.app.network.cashier.RegisterLifecycleEventType

class RegisterSessionManagerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = FakeContext()
        resetSingleton()
    }

    @After
    fun tearDown() {
        resetSingleton()
    }

    @Test
    fun `open request close confirm follows lifecycle transitions`() {
        val manager = RegisterSessionManager.getInstance(context)

        val opened = manager.openSession("event-1", "register-1")
        assertEquals(RegisterSessionManager.State.OPEN, opened.state)
        assertEquals(RegisterLifecycleEventType.REGISTER_LIFECYCLE_OPEN, opened.pendingLifecycleEvent)

        assertTrue(manager.requestClose())
        val afterRequest = manager.getCurrent()
        assertNotNull(afterRequest)
        assertEquals(RegisterSessionManager.State.CLOSE_REQUESTED, afterRequest!!.state)
        assertEquals(
            RegisterLifecycleEventType.REGISTER_LIFECYCLE_CLOSE_REQUESTED,
            afterRequest.pendingLifecycleEvent,
        )

        assertTrue(manager.confirmClose())
        val afterConfirm = manager.getCurrent()
        assertNotNull(afterConfirm)
        assertEquals(RegisterSessionManager.State.CLOSE_REQUESTED, afterConfirm!!.state)
        assertEquals(
            RegisterLifecycleEventType.REGISTER_LIFECYCLE_CLOSE_CONFIRMED,
            afterConfirm.pendingLifecycleEvent,
        )

        manager.clearPendingLifecycleEvent(
            expectedLifecycleEvent = RegisterLifecycleEventType.REGISTER_LIFECYCLE_CLOSE_CONFIRMED,
            expectedSessionId = afterConfirm.sessionId,
        )
        val afterClear = manager.getCurrent()
        assertNotNull(afterClear)
        assertEquals(RegisterSessionManager.State.CLOSED, afterClear!!.state)
    }

    @Test
    fun `clearPendingLifecycleEvent clears only matching in-flight payload`() {
        val manager = RegisterSessionManager.getInstance(context)
        val opened = manager.openSession("event-1", "register-1")

        manager.clearPendingLifecycleEvent(
            expectedLifecycleEvent = RegisterLifecycleEventType.REGISTER_LIFECYCLE_SYNC,
            expectedSessionId = opened.sessionId,
        )
        assertEquals(
            RegisterLifecycleEventType.REGISTER_LIFECYCLE_OPEN,
            manager.getCurrent()!!.pendingLifecycleEvent,
        )

        manager.clearPendingLifecycleEvent(
            expectedLifecycleEvent = RegisterLifecycleEventType.REGISTER_LIFECYCLE_OPEN,
            expectedSessionId = "another-session",
        )
        assertEquals(
            RegisterLifecycleEventType.REGISTER_LIFECYCLE_OPEN,
            manager.getCurrent()!!.pendingLifecycleEvent,
        )

        manager.clearPendingLifecycleEvent(
            expectedLifecycleEvent = RegisterLifecycleEventType.REGISTER_LIFECYCLE_OPEN,
            expectedSessionId = opened.sessionId,
        )
        assertEquals(null, manager.getCurrent()!!.pendingLifecycleEvent)
    }

    @Test
    fun `recordSync does not overwrite pending transition event`() {
        val manager = RegisterSessionManager.getInstance(context)

        manager.openSession("event-1", "register-1")
        manager.recordSync()

        assertEquals(
            RegisterLifecycleEventType.REGISTER_LIFECYCLE_OPEN,
            manager.getCurrent()!!.pendingLifecycleEvent,
        )
    }

    @Test
    fun `session is restored from shared preferences`() {
        val manager = RegisterSessionManager.getInstance(context)
        val opened = manager.openSession("event-1", "register-1")

        resetSingleton()
        val restoredManager = RegisterSessionManager.getInstance(context)
        val restored = restoredManager.getCurrent()

        assertNotNull(restored)
        assertEquals(opened.sessionId, restored!!.sessionId)
        assertEquals("event-1", restored.eventId)
        assertEquals("register-1", restored.registerId)
        assertEquals(RegisterSessionManager.State.OPEN, restored.state)
    }

    private fun resetSingleton() {
        val instanceField = RegisterSessionManager::class.java.getDeclaredField("instance")
        instanceField.isAccessible = true
        instanceField.set(null, null)
    }

    private class FakeContext : ContextWrapper(null) {
        private val prefs = mutableMapOf<String, FakeSharedPreferences>()

        override fun getApplicationContext(): Context = this

        override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences {
            val key = name ?: "default"
            return prefs.getOrPut(key) { FakeSharedPreferences() }
        }
    }

    private class FakeSharedPreferences : SharedPreferences {
        private val values = mutableMapOf<String, String?>()

        override fun getString(key: String?, defValue: String?): String? = values[key] ?: defValue

        override fun edit(): SharedPreferences.Editor = Editor(values)

        override fun getAll(): MutableMap<String, *> = values.toMutableMap()

        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = defValues

        override fun getInt(key: String?, defValue: Int): Int = defValue

        override fun getLong(key: String?, defValue: Long): Long = defValue

        override fun getFloat(key: String?, defValue: Float): Float = defValue

        override fun getBoolean(key: String?, defValue: Boolean): Boolean = defValue

        override fun contains(key: String?): Boolean = key != null && values.containsKey(key)

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        private class Editor(private val values: MutableMap<String, String?>) : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, String?>()
            private var clearRequested = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                if (key != null) {
                    pending[key] = value
                }
                return this
            }

            override fun apply() {
                commit()
            }

            override fun commit(): Boolean {
                if (clearRequested) {
                    values.clear()
                }
                pending.forEach { (k, v) -> values[k] = v }
                pending.clear()
                clearRequested = false
                return true
            }

            override fun clear(): SharedPreferences.Editor {
                clearRequested = true
                return this
            }

            override fun remove(key: String?): SharedPreferences.Editor {
                if (key != null) {
                    pending[key] = null
                }
                return this
            }

            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = this

            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = this

            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = this

            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = this

            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = this
        }
    }
}
