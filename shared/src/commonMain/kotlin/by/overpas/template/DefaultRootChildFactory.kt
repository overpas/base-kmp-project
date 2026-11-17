package by.overpas.template

import by.overpas.template.DefaultRootComponent.Config
import by.overpas.template.di.RootChildBuilder
import by.overpas.template.di.RootGraphProviders
import com.arkivanov.decompose.ComponentContext
import kotlin.reflect.KClass

internal fun interface RootChildFactory<Config> {

    fun create(
        config: Config,
        componentContext: ComponentContext,
        rootGraphProviders: RootGraphProviders,
    ): RootComponent.Child
}

internal class DefaultRootChildFactory(
    private val childBuilders: Map<KClass<*>, RootChildBuilder>,
) : RootChildFactory<Config> {

    override fun create(
        config: Config,
        componentContext: ComponentContext,
        rootGraphProviders: RootGraphProviders,
    ): RootComponent.Child =
        childBuilders.getValue(config::class)
            .build(
                config = config,
                componentContext = componentContext,
                rootGraphProviders = rootGraphProviders,
            )
}
