package by.overpas.template.di

import com.arkivanov.decompose.ComponentContext
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Includes
import dev.zacsweers.metro.Provides

@DependencyGraph(RootScope::class, bindingContainers = [RootProviders::class])
internal interface RootGraph :
    RootEntryPoint,
    RootGraphProviders {

    @DependencyGraph.Factory
    interface Factory {

        fun create(
            @Provides componentContext: ComponentContext,
            @Includes rootDependencies: RootDependencies,
        ): RootGraph
    }
}
