package net.animetone

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import net.animetone.predictionSettings.PredictionSettingsViewModel
import net.animetone.userStats.StatsViewModel
import timber.log.Timber

class KeyboardGPTApp: Application() {

    private lateinit var viewModelProvider: ViewModelProvider

    lateinit var predictionSettingsViewModel: PredictionSettingsViewModel
    lateinit var statsViewModel: StatsViewModel
    lateinit var activityTracker: ActivityTracker

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())

        activityTracker = ActivityTracker()
        registerActivityLifecycleCallbacks(activityTracker)

        // Instantiating here to allow for the earliest possible instantiation time
        predictionSettingsViewModel = getViewModelProvider()[PredictionSettingsViewModel::class.java]
        statsViewModel = getViewModelProvider()[StatsViewModel::class.java]
    }

    private fun getViewModelProvider(): ViewModelProvider {
        if (!::viewModelProvider.isInitialized) {
            viewModelProvider = ViewModelProvider(ViewModelStore(), ViewModelProvider.NewInstanceFactory())
        }
        return viewModelProvider
    }

}