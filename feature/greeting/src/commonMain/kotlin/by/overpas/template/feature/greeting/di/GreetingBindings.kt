package by.overpas.template.feature.greeting.di

import by.overpas.template.feature.greeting.DefaultGreetingComponent
import by.overpas.template.feature.greeting.GreetingComponent
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds

@BindingContainer
internal interface GreetingBindings {

    @Binds
    val DefaultGreetingComponent.Factory.bind: GreetingComponent.Factory
}
