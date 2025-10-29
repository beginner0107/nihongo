package com.nihongo.conversation

import android.app.Application
import com.nihongo.conversation.core.util.DataInitializer
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class NihongoApp : Application() {

    @Inject
    lateinit var dataInitializer: DataInitializer

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            dataInitializer.initializeDefaultData()
        }
    }
}
