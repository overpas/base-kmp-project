package by.overpas.template

import by.overpas.template.DefaultRootComponent.Config
import by.overpas.template.di.RootGraphProviders
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value

internal interface RootNavigator {

    val stack: Value<ChildStack<*, RootComponent.Child>>
}

internal class RootStackNavigator(
    componentContext: ComponentContext,
    rootGraphProviders: RootGraphProviders,
    private val childFactory: RootChildFactory<Config>,
    stackNavigation: StackNavigation<Config>,
) : RootNavigator {

    override val stack: Value<ChildStack<*, RootComponent.Child>> =
        componentContext.childStack(
            source = stackNavigation,
            serializer = Config.serializer(),
            initialStack = { listOf(Config.Hello) },
            handleBackButton = true,
            childFactory = { config, componentContext ->
                childFactory.create(
                    config = config,
                    componentContext = componentContext,
                    rootGraphProviders = rootGraphProviders,
                )
            },
        )
}
