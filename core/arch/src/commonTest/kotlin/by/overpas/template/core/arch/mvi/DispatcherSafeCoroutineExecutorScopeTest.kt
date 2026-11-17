package by.overpas.template.core.arch.mvi

import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutorScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals

class DispatcherSafeCoroutineExecutorScopeTest {

    private val testDispatcher = StandardTestDispatcher()
    private val delegate = object : CoroutineExecutorScope<Unit, Unit, Unit, Unit> {

        val methodCalls = mutableMapOf<String, Int>()

        override val coroutineContext: CoroutineContext =
            testDispatcher

        override fun dispatch(message: Unit) {
            methodCalls["dispatch"] = (methodCalls["dispatch"] ?: 0) + 1
        }

        override fun forward(action: Unit) {
            methodCalls["forward"] = (methodCalls["forward"] ?: 0) + 1
        }

        override fun publish(label: Unit) {
            methodCalls["publish"] = (methodCalls["publish"] ?: 0) + 1
        }

        override fun state() =
            Unit
    }

    private val sut: SafeCoroutineExecutorScope<Unit, Unit, Unit, Unit> = delegate.safeIn(
        testDispatcher,
    )

    @Test
    fun `post dispatch calls dispatch`() =
        runTest(testDispatcher) {
            sut.safeDispatch(Unit)
            val actual = delegate.methodCalls.getValue("dispatch")

            assertEquals(1, actual)
        }

    @Test
    fun `post forward calls forward`() =
        runTest(testDispatcher) {
            sut.safeForward(Unit)
            val actual = delegate.methodCalls.getValue("forward")

            assertEquals(1, actual)
        }

    @Test
    fun `post publish calls publish`() =
        runTest(testDispatcher) {
            sut.safePublish(Unit)
            val actual = delegate.methodCalls.getValue("publish")

            assertEquals(1, actual)
        }

    @Test
    fun `gets state`() =
        runTest(testDispatcher) {
            val actual = sut.state

            assertEquals(Unit, actual)
        }
}
