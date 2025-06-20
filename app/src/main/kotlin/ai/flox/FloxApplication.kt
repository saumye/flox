package ai.flox

import ai.flox.home.worker.NewsProcessingWorker
import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.WorkManager
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

@HiltAndroidApp
class FloxApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workManager: WorkManager

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface HiltWorkerFactoryEntryPoint {
        fun workerFactory(): HiltWorkerFactory
    }

    override fun onCreate() {
        super.onCreate()
        
        // Schedule TTS audio generation work
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        workManager.enqueueUniqueWork(
            NewsProcessingWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            androidx.work.OneTimeWorkRequestBuilder<NewsProcessingWorker>()
                .setConstraints(constraints)
                .build()
        )
    }

    override val workManagerConfiguration = Configuration.Builder()
        .setWorkerFactory(EntryPoints.get(this, HiltWorkerFactoryEntryPoint::class.java).workerFactory())
        .build()
} 