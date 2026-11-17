package by.overpas.template

import by.overpas.template.DefaultRootComponent.Config
import by.overpas.template.di.RootGraph
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import dev.zacsweers.metro.createGraphFactory
import kotlin.test.Test
import kotlin.test.assertEquals

internal class RootStackRestoreTest {

    private val stateKeeper = StateKeeperDispatcher()
    private val previousGraph = createGraphFactory<RootGraph.Factory>()
        .create(
            componentContext = DefaultComponentContext(
                lifecycle = LifecycleRegistry(),
                stateKeeper = stateKeeper,
            ),
            rootDependencies = FakeRootDependencies(),
        )
    private val previous: RootComponent = previousGraph.rootComponent
    private val sut: RootComponent by lazy {
        createGraphFactory<RootGraph.Factory>()
            .create(
                componentContext = DefaultComponentContext(
                    lifecycle = LifecycleRegistry(),
                    stateKeeper = StateKeeperDispatcher(stateKeeper.save()),
                ),
                rootDependencies = FakeRootDependencies(),
            )
            .rootComponent
    }

    @Test
    fun `restored stack keeps Greeting on top of Hello`() {
        val expected = listOf(Config.Hello, Config.Greeting("Ada"))
        previousGraph.helloNavigator.openGreeting("Ada")

        val actual = sut.stack.value
            .items
            .map { child -> child.configuration }

        assertEquals(expected, actual)
    }
}
