package by.overpas.template

import by.overpas.template.DefaultRootComponent.Config
import by.overpas.template.di.RootChildBuilder
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import dev.zacsweers.metro.createGraphFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

internal class DefaultRootChildFactoryTest {

    private val graphContext = DefaultComponentContext(LifecycleRegistry())
    private val rootGraphProviders = createGraphFactory<TestRootGreetingNavigatorGraph.Factory>()
        .create(
            componentContext = graphContext,
            rootDependencies = FakeRootDependencies(),
        )

    private val helloChild = RootComponent.Child.Hello(component = FakeHelloComponent())
    private val builtConfigs = mutableListOf<Config>()

    private val sut = DefaultRootChildFactory(
        childBuilders = mapOf(
            Config.Hello::class to RootChildBuilder { config, _, _ ->
                builtConfigs += config
                helloChild
            },
        ),
    )

    @Test
    fun `Hello config builds the hello child`() {
        val actual = sut.create(
            config = Config.Hello,
            componentContext = DefaultComponentContext(LifecycleRegistry()),
            rootGraphProviders = rootGraphProviders,
        )

        assertSame(helloChild, actual)
    }

    @Test
    fun `the builder gets the config`() {
        val expected = listOf<Config>(Config.Hello)

        sut.create(
            config = Config.Hello,
            componentContext = DefaultComponentContext(LifecycleRegistry()),
            rootGraphProviders = rootGraphProviders,
        )

        assertEquals(expected, builtConfigs)
    }

    @Test
    fun `an unregistered config fails`() {
        val actual = runCatching {
            sut.create(
                config = Config.Greeting("Ada"),
                componentContext = DefaultComponentContext(LifecycleRegistry()),
                rootGraphProviders = rootGraphProviders,
            )
        }.exceptionOrNull()

        assertIs<NoSuchElementException>(actual)
    }
}
