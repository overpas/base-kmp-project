package by.overpas.template.di

import by.overpas.template.DefaultRootComponent.Config
import by.overpas.template.RootComponent
import com.arkivanov.decompose.ComponentContext

internal fun interface RootChildBuilder {

    fun build(
        config: Config,
        componentContext: ComponentContext,
        rootGraphProviders: RootGraphProviders,
    ): RootComponent.Child
}
