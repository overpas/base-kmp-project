package by.overpas.template.feature.greeting.ui

import androidx.compose.ui.test.ExperimentalTestApi
import by.overpas.template.core.ui.test.ComposeUiTestBase
import by.overpas.template.feature.greeting.FakeGreetingNavigator
import by.overpas.template.feature.greeting.FakeGreetingRootDependencies
import by.overpas.template.feature.greeting.di.GreetingGraph
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.atiurin.ultron.core.compose.runUltronUiTest
import dev.zacsweers.metro.createGraphFactory
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class GreetingUiTest : ComposeUiTestBase() {

    private val navigator = FakeGreetingNavigator()
    private val graph = createGraphFactory<GreetingGraph.Factory>()
        .create(rootDependencies = FakeGreetingRootDependencies(greetingNavigator = navigator))
    private val component = graph.componentFactory.create(
        componentContext = DefaultComponentContext(
            LifecycleRegistry(initialState = Lifecycle.State.CREATED),
        ),
        name = "Ada",
    )

    @Test
    fun testGreetingShowsTheName() =
        runUltronUiTest {
            with(GreetingPage) {
                setContent(component)

                assertMessageContains("Ada")
            }
        }

    @Test
    fun testBackNavigatesBack() =
        runUltronUiTest {
            with(GreetingPage) {
                setContent(component)

                clickBack()

                assertTrue(navigator.isNavigateBackCalled)
            }
        }
}
