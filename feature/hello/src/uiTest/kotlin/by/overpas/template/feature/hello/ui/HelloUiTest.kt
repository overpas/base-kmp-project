package by.overpas.template.feature.hello.ui

import androidx.compose.ui.test.ExperimentalTestApi
import by.overpas.template.core.ui.test.ComposeUiTestBase
import by.overpas.template.feature.hello.FakeHelloAppDependencies
import by.overpas.template.feature.hello.FakeHelloNavigator
import by.overpas.template.feature.hello.FakeHelloRootDependencies
import by.overpas.template.feature.hello.di.HelloGraph
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.atiurin.ultron.core.compose.runUltronUiTest
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class HelloUiTest : ComposeUiTestBase() {

    private val componentContext = DefaultComponentContext(
        LifecycleRegistry(initialState = Lifecycle.State.CREATED),
    )
    private val testDispatcher = UnconfinedTestDispatcher()
    private val navigator = FakeHelloNavigator()
    private val graph = createGraphFactory<HelloGraph.Factory>()
        .create(
            dependencies = FakeHelloAppDependencies(mainDispatcher = testDispatcher),
            rootDependencies = FakeHelloRootDependencies(helloNavigator = navigator),
        )
    private val component = graph.componentFactory.create(componentContext)

    @Test
    fun testHelloIsDisplayed() =
        runUltronUiTest {
            with(HelloPage) {
                setContent(component)

                assertRootContainerIsDisplayed()
            }
        }

    @Test
    fun testGreetIsNotEnabledWithoutName() =
        runUltronUiTest {
            with(HelloPage) {
                setContent(component)

                assertGreetButtonIsNotEnabled()
            }
        }

    @Test
    fun testNameIsUpdated() =
        runUltronUiTest {
            with(HelloPage) {
                setContent(component)

                typeName("Ada")

                assertNameContains("Ada")
                assertGreetButtonIsEnabled()
            }
        }

    @Test
    fun testGreetOpensGreeting() =
        runUltronUiTest {
            with(HelloPage) {
                setContent(component)
                typeName("Ada")

                clickGreet()

                waitUntil { navigator.openedGreetings == listOf("Ada") }
            }
        }
}
