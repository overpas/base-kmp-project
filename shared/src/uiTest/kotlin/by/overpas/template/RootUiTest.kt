package by.overpas.template

import androidx.compose.ui.test.ExperimentalTestApi
import by.overpas.template.core.ui.test.ComposeUiTestBase
import by.overpas.template.di.RootGraph
import by.overpas.template.feature.greeting.ui.GreetingPage
import by.overpas.template.feature.hello.ui.HelloPage
import by.overpas.template.ui.RootPage
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.atiurin.ultron.core.compose.runUltronUiTest
import dev.zacsweers.metro.createGraphFactory
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class RootUiTest : ComposeUiTestBase() {

    private val componentContext = DefaultComponentContext(
        LifecycleRegistry(initialState = Lifecycle.State.CREATED),
    )
    private val rootGraph = createGraphFactory<RootGraph.Factory>()
        .create(
            componentContext = componentContext,
            rootDependencies = FakeRootDependencies(),
        )
    private val component = rootGraph.rootComponent

    @Test
    fun testRootShowsHello() =
        runUltronUiTest {
            with(RootPage) {
                setContent(component)

                assertChildrenDisplayed()
                HelloPage.assertRootContainerIsDisplayed()
            }
        }

    @Test
    fun testRootShowsGreeting() =
        runUltronUiTest {
            rootGraph.helloNavigator.openGreeting("Ada")

            with(RootPage) {
                setContent(component)

                GreetingPage.assertMessageContains("Ada")
            }
        }
}
