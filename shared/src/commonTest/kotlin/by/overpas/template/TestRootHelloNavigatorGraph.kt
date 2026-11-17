package by.overpas.template

import by.overpas.template.di.RootDependencies
import by.overpas.template.di.RootEntryPoint
import by.overpas.template.di.RootGraphProviders
import by.overpas.template.di.RootProviders
import by.overpas.template.di.RootScope
import com.arkivanov.decompose.ComponentContext
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Includes
import dev.zacsweers.metro.Provides

@DependencyGraph(RootScope::class, bindingContainers = [RootProviders::class])
internal interface TestRootHelloNavigatorGraph :
    RootEntryPoint,
    RootGraphProviders {

    fun inject(test: RootHelloNavigatorTest)

    @DependencyGraph.Factory
    interface Factory {

        fun create(
            @Provides componentContext: ComponentContext,
            @Includes rootDependencies: RootDependencies,
        ): TestRootHelloNavigatorGraph
    }
}
