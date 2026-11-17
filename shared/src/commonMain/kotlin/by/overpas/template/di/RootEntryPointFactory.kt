package by.overpas.template.di

import com.arkivanov.decompose.ComponentContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.createGraphFactory

@Inject
@ContributesBinding(AppScope::class)
internal class RootEntryPointFactory(private val rootDependencies: RootDependencies) :
    RootEntryPoint.Factory {

    override fun create(componentContext: ComponentContext): RootEntryPoint =
        createGraphFactory<RootGraph.Factory>()
            .create(
                componentContext = componentContext,
                rootDependencies = rootDependencies,
            )
}
