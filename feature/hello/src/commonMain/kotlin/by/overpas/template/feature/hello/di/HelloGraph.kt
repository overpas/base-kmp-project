package by.overpas.template.feature.hello.di

import by.overpas.template.feature.hello.HelloComponent
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Includes

@DependencyGraph(scope = HelloScope::class, bindingContainers = [HelloBindings::class])
interface HelloGraph {

    val componentFactory: HelloComponent.Factory

    @DependencyGraph.Factory
    fun interface Factory {

        fun create(
            @Includes dependencies: HelloAppDependencies,
            @Includes rootDependencies: HelloRootDependencies,
        ): HelloGraph
    }
}
