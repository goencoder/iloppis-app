package se.iloppis.app

import android.content.Context

/**
 * Minimal app-wide context access for background scheduling from ViewModels.
 *
 * This is a stopgap until we introduce DI.
 */
object ILoppisAppHolder {
    lateinit var appContext: Context
        private set

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }
}
