package by.overpas.template.feature.hello

import by.overpas.template.core.test.collectCurrentOnBackground
import by.overpas.template.feature.hello.di.HelloGraph
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
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
class HelloComponentRestoreTest {

    private val testDispatcher = StandardTestDispatcher()
    private val graph = createGraphFactory<HelloGraph.Factory>()
        .create(
            dependencies = FakeHelloAppDependencies(mainDispatcher = testDispatcher),
            rootDependencies = FakeHelloRootDependencies(),
        )
    private val stateKeeper = StateKeeperDispatcher()
    private val previous: HelloComponent by lazy {
        graph.componentFactory.create(
            DefaultComponentContext(
                lifecycle = LifecycleRegistry(initialState = Lifecycle.State.CREATED),
                stateKeeper = stateKeeper,
            ),
        )
    }
    private val sut: HelloComponent by lazy {
        graph.componentFactory.create(
            DefaultComponentContext(
                lifecycle = LifecycleRegistry(initialState = Lifecycle.State.CREATED),
                stateKeeper = StateKeeperDispatcher(stateKeeper.save()),
            ),
        )
    }

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `name is restored and greet is enabled after process death`() =
        runTest(testDispatcher) {
            val expected = HelloState(name = "Ada", isGreetEnabled = true)
            previous.state.collectCurrentOnBackground()
            previous.onNameChanged("Ada")
            advanceUntilIdle()

            sut.state.collectCurrentOnBackground()
            advanceUntilIdle()
            val actual = sut.state.value

            assertEquals(expected, actual)
        }
}
