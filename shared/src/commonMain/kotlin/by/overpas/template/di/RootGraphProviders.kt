package by.overpas.template.di

import by.overpas.template.DefaultRootComponent
import by.overpas.template.RootChildFactory
import by.overpas.template.RootNavigator
import by.overpas.template.RootStackNavigator
import by.overpas.template.feature.greeting.di.GreetingRootDependencies
import by.overpas.template.feature.hello.di.HelloRootDependencies
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

internal interface RootGraphProviders :
    HelloRootDependencies,
    GreetingRootDependencies {

    @Provides
    @SingleIn(RootScope::class)
    fun rootNavigator(
        componentContext: ComponentContext,
        childFactory: RootChildFactory<DefaultRootComponent.Config>,
        stackNavigation: StackNavigation<DefaultRootComponent.Config>,
    ): RootNavigator =
        RootStackNavigator(
            componentContext = componentContext,
            rootGraphProviders = this,
            childFactory = childFactory,
            stackNavigation = stackNavigation,
        )
}
