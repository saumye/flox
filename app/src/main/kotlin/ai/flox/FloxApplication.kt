package ai.flox

import ai.flox.home.worker.NewsProcessingWorker
import android.R.attr.delay
import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import org.joda.time.DateTime
import org.joda.time.Duration
import java.util.concurrent.TimeUnit
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

        val delay = NewsProcessingWorker.Companion.calculateRandomDelayUntilMorningCalendar()

        workManager.enqueueUniquePeriodicWork(
            NewsProcessingWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequest.Builder(
                NewsProcessingWorker::class.java,
                24,
                TimeUnit.HOURS,
                PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS,
                TimeUnit.MILLISECONDS
            )
                .setConstraints(constraints)
                .setInitialDelay(delay, TimeUnit.MINUTES)
                .addTag(NewsProcessingWorker.WORK_NAME)
                .build()
        )
    }

    override val workManagerConfiguration = Configuration.Builder()
        .setWorkerFactory(EntryPoints.get(this, HiltWorkerFactoryEntryPoint::class.java).workerFactory())
        .build()
} 