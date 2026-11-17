package by.overpas.template.feature.greeting

import by.overpas.template.feature.greeting.di.GreetingGraph
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import dev.zacsweers.metro.createGraphFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GreetingComponentTest {

    private val navigator = FakeGreetingNavigator()
    private val graph = createGraphFactory<GreetingGraph.Factory>()
        .create(rootDependencies = FakeGreetingRootDependencies(greetingNavigator = navigator))

    private val sut = graph.componentFactory.create(
        componentContext = DefaultComponentContext(LifecycleRegistry()),
        name = "Ada",
    )

    @Test
    fun `state holds the name`() {
        val expected = GreetingState(name = "Ada")

        val actual = sut.state.value

        assertEquals(expected, actual)
    }

    @Test
    fun `back click navigates back`() {
        sut.onBackClick()

        assertTrue(navigator.isNavigateBackCalled)
    }
}
