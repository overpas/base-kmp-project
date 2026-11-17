package by.overpas.template

import android.app.Application
import by.overpas.template.core.platform.di.AndroidPlatformGraph
import by.overpas.template.di.AppEntryPoint
import by.overpas.template.di.createAppEntryPoint
import dev.zacsweers.metro.createGraphFactory

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        appEntryPoint = createGraphFactory<AndroidPlatformGraph.Factory>()
            .create(applicationContext)
            .createAppEntryPoint()
    }

    companion object {

        lateinit var appEntryPoint: AppEntryPoint
    }
}
