package by.overpas.template.feature.hello.input.handlers

import by.overpas.template.core.arch.mvi.TestSafeCoroutineExecutorScope
import by.overpas.template.feature.hello.input.InputStore.Action
import by.overpas.template.feature.hello.input.InputStore.Label
import by.overpas.template.feature.hello.input.InputStore.Message
import by.overpas.template.feature.hello.input.InputStore.State
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlin.test.Test
import kotlin.test.assertContains

internal class CheckInputHandlerTest {

    private val stateFlow = MutableStateFlow(State())
    private val testDispatcher = StandardTestDispatcher()
    private val testSafeCoroutineExecutorScope =
        TestSafeCoroutineExecutorScope<State, Message, Action, Label>(
            stateFlow = stateFlow,
            context = testDispatcher,
        )
    private val sut = CheckInputHandler()

    @Test
    fun `greet is enabled for a name`() =
        with(testSafeCoroutineExecutorScope) {
            stateFlow.value = State(name = "Ada")
            val expected = Message.InputChecked(isGreetEnabled = true)

            sut.runWith(Action.CheckInput)
            testDispatcher.scheduler.runCurrent()

            assertContains(dispatchedMessages, expected)
        }

    @Test
    fun `greet is disabled for a blank name`() =
        with(testSafeCoroutineExecutorScope) {
            stateFlow.value = State(name = " ")
            val expected = Message.InputChecked(isGreetEnabled = false)

            sut.runWith(Action.CheckInput)
            testDispatcher.scheduler.runCurrent()

            assertContains(dispatchedMessages, expected)
        }
}
