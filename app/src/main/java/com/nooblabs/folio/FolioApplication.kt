package com.nooblabs.folio
import android.app.Application

import com.nooblabs.folio.di.AppContainer
import com.nooblabs.folio.di.DefaultAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class FolioApplication : Application() {
    /** Application-level coroutine scope — lives for the entire process lifetime. */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
