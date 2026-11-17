package by.overpas.template

import by.overpas.template.di.RootDependencies

internal class FakeRootDependencies(
    override val rootChildFactory: RootChildFactory<DefaultRootComponent.Config> =
        FakeDefaultRootChildFactory.create(),
) : RootDependencies
