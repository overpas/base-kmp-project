package by.overpas.template

import by.overpas.template.DefaultRootComponent.Config
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import dev.zacsweers.metro.createGraphFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

internal class RootStackNavigatorTest {

    private val graphContext = DefaultComponentContext(LifecycleRegistry())
    private val graph = createGraphFactory<TestRootGreetingNavigatorGraph.Factory>()
        .create(
            componentContext = graphContext,
            rootDependencies = FakeRootDependencies(),
        )

    private val stackNavigation = StackNavigation<Config>()
    private val backDispatcher = BackDispatcher()
    private val sut = RootStackNavigator(
        componentContext = DefaultComponentContext(
            lifecycle = LifecycleRegistry(),
            backHandler = backDispatcher,
        ),
        rootGraphProviders = graph,
        childFactory = FakeDefaultRootChildFactory.create(),
        stackNavigation = stackNavigation,
    )

    @Test
    fun `the stack starts on Hello`() {
        val actual = sut.stack.value
            .active
            .instance

        assertIs<RootComponent.Child.Hello>(actual)
    }

    @Test
    fun `the stack starts with a single entry`() {
        val actual = sut.stack.value
            .items
            .size

        assertEquals(1, actual)
    }

    @Test
    fun `back from Greeting returns to Hello`() {
        stackNavigation.pushNew(Config.Greeting("Ada"))

        backDispatcher.back()
        val actual = sut.stack.value
            .active
            .instance

        assertIs<RootComponent.Child.Hello>(actual)
    }
}
