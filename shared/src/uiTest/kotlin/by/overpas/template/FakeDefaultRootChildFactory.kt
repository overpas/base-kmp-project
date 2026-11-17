package by.overpas.template

import by.overpas.template.DefaultRootComponent.Config
import by.overpas.template.di.RootGraphProviders
import com.arkivanov.decompose.ComponentContext

internal class FakeDefaultRootChildFactory : RootChildFactory<Config> {

    override fun create(
        config: Config,
        componentContext: ComponentContext,
        rootGraphProviders: RootGraphProviders,
    ): RootComponent.Child =
        when (config) {
            is Config.Hello -> RootComponent.Child.Hello(
                component = FakeHelloComponent(),
            )

            is Config.Greeting -> RootComponent.Child.Greeting(
                component = FakeGreetingComponent(
                    name = config.name,
                    navigator = rootGraphProviders.greetingNavigator,
                ),
            )
        }

    companion object {

        fun create(): RootChildFactory<Config> =
            FakeDefaultRootChildFactory()
    }
}
