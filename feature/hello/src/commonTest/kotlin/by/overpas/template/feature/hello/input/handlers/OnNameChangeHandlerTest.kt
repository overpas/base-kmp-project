package by.overpas.template.feature.hello.input.handlers

import by.overpas.template.core.arch.mvi.TestSafeCoroutineExecutorScope
import by.overpas.template.feature.hello.input.InputStore.Action
import by.overpas.template.feature.hello.input.InputStore.Intent
import by.overpas.template.feature.hello.input.InputStore.Label
import by.overpas.template.feature.hello.input.InputStore.Message
import by.overpas.template.feature.hello.input.InputStore.State
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlin.test.Test
import kotlin.test.assertContains

internal class OnNameChangeHandlerTest {

    private val stateFlow = MutableStateFlow(State())
    private val testDispatcher = StandardTestDispatcher()
    private val testSafeCoroutineExecutorScope =
        TestSafeCoroutineExecutorScope<State, Message, Action, Label>(
            stateFlow = stateFlow,
            context = testDispatcher,
        )
    private val sut = OnNameChangeHandler()

    @Test
    fun `Message NameChanged is dispatched`() =
        with(testSafeCoroutineExecutorScope) {
            val expected = Message.NameChanged("Ada")

            sut.runWith(Intent.OnNameChange("Ada"))
            testDispatcher.scheduler.runCurrent()

            assertContains(dispatchedMessages, expected)
        }

    @Test
    fun `input check is forwarded`() =
        with(testSafeCoroutineExecutorScope) {
            val expected = Action.CheckInput

            sut.runWith(Intent.OnNameChange("Ada"))
            testDispatcher.scheduler.runCurrent()

            assertContains(forwardedActions, expected)
        }
}
