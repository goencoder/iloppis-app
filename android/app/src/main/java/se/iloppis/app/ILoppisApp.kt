package se.iloppis.app

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import se.iloppis.app.data.CommittedScansStore
import se.iloppis.app.data.MigrationManager
import se.iloppis.app.data.PendingItemsStore
import se.iloppis.app.data.PendingScansStore
import se.iloppis.app.data.RejectedPurchaseStore
import se.iloppis.app.data.SoldItemFileStore

class ILoppisApp : Application() {
	private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
	
	override fun onCreate() {
		super.onCreate()

		ILoppisAppHolder.initialize(this)
		
		// Note: File stores now require eventId and are initialized lazily
		// when ViewModels/Workers access them for a specific event
		
		// Run one-time migration from old system
		applicationScope.launch {
			MigrationManager.runMigrationIfNeeded(this@ILoppisApp)
		}
	}
}
