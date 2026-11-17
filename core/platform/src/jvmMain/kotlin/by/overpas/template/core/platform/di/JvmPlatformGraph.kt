package by.overpas.template.core.platform.di

import by.overpas.template.core.platform.PlatformApi
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import java.io.File
import java.util.Properties

@DependencyGraph(AppScope::class, bindingContainers = [JvmPlatformProviders::class])
interface JvmPlatformGraph : PlatformApi {

    @DependencyGraph.Factory
    fun interface Factory {

        fun create(
            @Provides properties: Properties,
            @Provides savedStateFile: File,
        ): JvmPlatformGraph
    }
}
