package by.overpas.template.feature.greeting.di

import by.overpas.template.feature.greeting.GreetingComponent
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Includes

@DependencyGraph(scope = GreetingScope::class, bindingContainers = [GreetingBindings::class])
interface GreetingGraph {

    val componentFactory: GreetingComponent.Factory

    @DependencyGraph.Factory
    fun interface Factory {

        fun create(@Includes rootDependencies: GreetingRootDependencies): GreetingGraph
    }
}
