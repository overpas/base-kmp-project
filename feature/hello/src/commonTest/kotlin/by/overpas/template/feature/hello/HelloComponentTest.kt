package by.overpas.template.feature.hello

import by.overpas.template.core.test.collectCurrentOnBackground
import by.overpas.template.feature.hello.di.HelloGraph
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class HelloComponentTest {

    private val componentContext = DefaultComponentContext(
        LifecycleRegistry(initialState = Lifecycle.State.CREATED),
    )
    private val testDispatcher = StandardTestDispatcher()
    private val navigator = FakeHelloNavigator()
    private val graph = createGraphFactory<HelloGraph.Factory>()
        .create(
            dependencies = FakeHelloAppDependencies(mainDispatcher = testDispatcher),
            rootDependencies = FakeHelloRootDependencies(helloNavigator = navigator),
        )

    private val sut: HelloComponent by lazy { graph.componentFactory.create(componentContext) }

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty`() {
        val expected = HelloState()

        val actual = sut.state.value

        assertEquals(expected, actual)
    }

    @Test
    fun `typed name enables greet`() =
        runTest(testDispatcher) {
            val expected = HelloState(name = "Ada", isGreetEnabled = true)
            sut.state.collectCurrentOnBackground()

            sut.onNameChanged("Ada")
            advanceUntilIdle()
            val actual = sut.state.value

            assertEquals(expected, actual)
        }

    @Test
    fun `greet click opens the greeting for the name`() =
        runTest(testDispatcher) {
            val expected = listOf("Ada")
            sut.state.collectCurrentOnBackground()
            sut.onNameChanged("Ada")
            advanceUntilIdle()

            sut.onGreetClick()
            advanceUntilIdle()
            val actual = navigator.openedGreetings

            assertEquals(expected, actual)
        }
}
