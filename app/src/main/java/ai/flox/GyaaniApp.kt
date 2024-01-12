package ai.flox

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FloxApp : Application() {

    override fun onCreate() {
        super.onCreate()
    }

}