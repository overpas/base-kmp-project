package by.overpas.template.core.platform.di

import android.content.Context
import by.overpas.template.core.platform.PlatformApi
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

@DependencyGraph(AppScope::class)
interface AndroidPlatformGraph : PlatformApi {

    @DependencyGraph.Factory
    fun interface Factory {

        fun create(@Provides context: Context): AndroidPlatformGraph
    }
}
