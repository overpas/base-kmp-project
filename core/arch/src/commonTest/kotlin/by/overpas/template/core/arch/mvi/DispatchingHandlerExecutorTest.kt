package by.overpas.template.core.arch.mvi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

private typealias TestScope = SafeCoroutineExecutorScope<String, String, TestAction, String>

class DispatchingHandlerExecutorTest {

    @Test
    fun `an intent goes to the handler registered for its own class`() =
        runTest {
            val sut = DispatchingHandlerExecutor(
                executionScope = CoroutineScope(UnconfinedTestDispatcher()),
                mainDispatcher = UnconfinedTestDispatcher(),
                intentHandlers = mapOf(
                    TestIntent.First::class to object :
                        IntentHandler<String, String, TestAction, String, TestIntent> {
                        override fun TestScope.handle(event: TestIntent) {
                            dispatch("from first")
                        }
                    },
                    TestIntent.Second::class to object :
                        IntentHandler<String, String, TestAction, String, TestIntent> {
                        override fun TestScope.handle(event: TestIntent) {
                            dispatch("from second")
                        }
                    },
                ),
            )
            val callbacks = FakeCallbacks()
            sut.init(callbacks)
            val expected = listOf("from second")

            sut.executeIntent(TestIntent.Second)

            assertEquals(expected, callbacks.messages)
        }

    @Test
    fun `an intent with no handler registered changes nothing`() =
        runTest {
            val sut = DispatchingHandlerExecutor<TestIntent, TestAction, String, String, String>(
                executionScope = CoroutineScope(UnconfinedTestDispatcher()),
                mainDispatcher = UnconfinedTestDispatcher(),
            )
            val callbacks = FakeCallbacks()
            sut.init(callbacks)
            val expected = emptyList<String>()

            sut.executeIntent(TestIntent.First)

            assertEquals(expected, callbacks.messages)
        }

    @Test
    fun `an action goes to the handler registered for its own class`() =
        runTest {
            val sut = DispatchingHandlerExecutor<TestIntent, TestAction, String, String, String>(
                executionScope = CoroutineScope(UnconfinedTestDispatcher()),
                mainDispatcher = UnconfinedTestDispatcher(),
                actionHandlers = mapOf(
                    TestAction.First::class to ActionHandler { dispatch("from first") },
                    TestAction.Second::class to ActionHandler { dispatch("from second") },
                ),
            )
            val callbacks = FakeCallbacks()
            sut.init(callbacks)
            val expected = listOf("from second")

            sut.executeAction(TestAction.Second)

            assertEquals(expected, callbacks.messages)
        }

    @Test
    fun `an action with no handler registered changes nothing`() =
        runTest {
            val sut = DispatchingHandlerExecutor<TestIntent, TestAction, String, String, String>(
                executionScope = CoroutineScope(UnconfinedTestDispatcher()),
                mainDispatcher = UnconfinedTestDispatcher(),
            )
            val callbacks = FakeCallbacks()
            sut.init(callbacks)
            val expected = emptyList<String>()

            sut.executeAction(TestAction.First)

            assertEquals(expected, callbacks.messages)
        }

    @Test
    fun `an action a handler forwards reaches the callbacks`() =
        runTest {
            val sut = DispatchingHandlerExecutor<TestIntent, TestAction, String, String, String>(
                executionScope = CoroutineScope(UnconfinedTestDispatcher()),
                mainDispatcher = UnconfinedTestDispatcher(),
                actionHandlers = mapOf(
                    TestAction.First::class to ActionHandler { forward(TestAction.Second) },
                ),
            )
            val callbacks = FakeCallbacks()
            sut.init(callbacks)
            val expected = listOf<TestAction>(TestAction.Second)

            sut.executeAction(TestAction.First)

            assertEquals(expected, callbacks.actions)
        }

    @Test
    fun `a label a handler publishes reaches the callbacks`() =
        runTest {
            val sut = DispatchingHandlerExecutor<TestIntent, TestAction, String, String, String>(
                executionScope = CoroutineScope(UnconfinedTestDispatcher()),
                mainDispatcher = UnconfinedTestDispatcher(),
                actionHandlers = mapOf(
                    TestAction.First::class to ActionHandler { publish("published") },
                ),
            )
            val callbacks = FakeCallbacks()
            sut.init(callbacks)
            val expected = listOf("published")

            sut.executeAction(TestAction.First)

            assertEquals(expected, callbacks.labels)
        }

    @Test
    fun `a handler reads the state the callbacks hold`() =
        runTest {
            val sut = DispatchingHandlerExecutor<TestIntent, TestAction, String, String, String>(
                executionScope = CoroutineScope(UnconfinedTestDispatcher()),
                mainDispatcher = UnconfinedTestDispatcher(),
                actionHandlers = mapOf(
                    TestAction.First::class to ActionHandler { dispatch(state()) },
                ),
            )
            val callbacks = FakeCallbacks(state = "held state")
            sut.init(callbacks)
            val expected = listOf("held state")

            sut.executeAction(TestAction.First)

            assertEquals(expected, callbacks.messages)
        }

    @Test
    fun `disposing cancels the scope handlers run in`() =
        runTest {
            val executionScope = CoroutineScope(UnconfinedTestDispatcher())
            val sut = DispatchingHandlerExecutor<TestIntent, TestAction, String, String, String>(
                executionScope = executionScope,
                mainDispatcher = UnconfinedTestDispatcher(),
            ).apply { init(FakeCallbacks()) }

            sut.dispose()

            assertFalse(executionScope.isActive)
        }
}
