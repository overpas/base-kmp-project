package by.overpas.template.feature.greeting

import by.overpas.template.feature.greeting.di.GreetingRootDependencies

class FakeGreetingRootDependencies(
    override val greetingNavigator: GreetingNavigator = FakeGreetingNavigator(),
) : GreetingRootDependencies
