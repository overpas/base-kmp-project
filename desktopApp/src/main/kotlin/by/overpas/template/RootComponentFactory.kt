package by.overpas.template

import by.overpas.template.core.platform.PlatformApi
import by.overpas.template.di.createAppEntryPoint
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackHandler
import com.arkivanov.essenty.lifecycle.Lifecycle

fun PlatformApi.createRootComponent(
    lifecycle: Lifecycle,
    backHandler: BackHandler,
): RootComponent {
    val componentContext = DefaultComponentContext(
        lifecycle = lifecycle,
        stateKeeper = stateSaver,
        backHandler = backHandler,
    )
    return createAppEntryPoint().rootEntryPointFactory.create(componentContext).rootComponent
}
