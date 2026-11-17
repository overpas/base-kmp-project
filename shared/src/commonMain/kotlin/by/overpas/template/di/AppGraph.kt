package by.overpas.template.di

import by.overpas.template.core.platform.PlatformApi
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Includes

@DependencyGraph(AppScope::class)
internal interface AppGraph :
    AppEntryPoint,
    AppGraphProviders {

    @DependencyGraph.Factory
    interface Factory {

        fun create(@Includes platformApi: PlatformApi): AppGraph
    }
}
