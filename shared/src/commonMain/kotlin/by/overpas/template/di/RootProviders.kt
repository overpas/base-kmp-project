package by.overpas.template.di

import by.overpas.template.DefaultRootComponent.Config
import com.arkivanov.decompose.router.stack.StackNavigation
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@BindingContainer
internal object RootProviders {

    @Provides
    @SingleIn(RootScope::class)
    fun stackNavigation(): StackNavigation<Config> =
        StackNavigation()
}
