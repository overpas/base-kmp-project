package by.overpas.template.di

import by.overpas.template.DefaultRootComponent
import by.overpas.template.RootChildFactory

internal interface RootDependencies {

    val rootChildFactory: RootChildFactory<DefaultRootComponent.Config>
}
