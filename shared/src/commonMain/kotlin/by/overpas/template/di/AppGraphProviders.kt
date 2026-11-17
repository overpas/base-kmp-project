package by.overpas.template.di

import by.overpas.template.DefaultRootChildFactory
import by.overpas.template.DefaultRootComponent
import by.overpas.template.RootChildFactory
import by.overpas.template.RootComponent
import by.overpas.template.feature.greeting.di.GreetingGraph
import by.overpas.template.feature.hello.di.HelloAppDependencies
import by.overpas.template.feature.hello.di.HelloGraph
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ClassKey
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraphFactory
import kotlin.reflect.KClass

internal interface AppGraphProviders :
    RootDependencies,
    HelloAppDependencies {

    @Provides
    @IntoMap
    @ClassKey(DefaultRootComponent.Config.Hello::class)
    @SingleIn(AppScope::class)
    fun helloChildBuilder(): RootChildBuilder =
        RootChildBuilder { _, componentContext, rootGraphProviders ->
            RootComponent.Child.Hello(
                component = createGraphFactory<HelloGraph.Factory>()
                    .create(
                        dependencies = this,
                        rootDependencies = rootGraphProviders,
                    )
                    .componentFactory
                    .create(componentContext),
            )
        }

    @Provides
    @IntoMap
    @ClassKey(DefaultRootComponent.Config.Greeting::class)
    @SingleIn(AppScope::class)
    fun greetingChildBuilder(): RootChildBuilder =
        RootChildBuilder { config, componentContext, rootGraphProviders ->
            require(config is DefaultRootComponent.Config.Greeting)
            RootComponent.Child.Greeting(
                component = createGraphFactory<GreetingGraph.Factory>()
                    .create(rootDependencies = rootGraphProviders)
                    .componentFactory
                    .create(
                        componentContext = componentContext,
                        name = config.name,
                    ),
            )
        }

    @Provides
    @SingleIn(AppScope::class)
    fun rootChildFactory(
        childBuilders: Map<KClass<*>, RootChildBuilder>,
    ): RootChildFactory<DefaultRootComponent.Config> =
        DefaultRootChildFactory(
            childBuilders = childBuilders,
        )

    @Provides
    @SingleIn(AppScope::class)
    fun rootDependencies(): RootDependencies =
        this
}
