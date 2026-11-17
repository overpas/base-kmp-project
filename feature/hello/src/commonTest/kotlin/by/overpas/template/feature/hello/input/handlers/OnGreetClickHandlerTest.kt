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
import kotlin.test.assertEquals

internal class OnGreetClickHandlerTest {

    private val stateFlow = MutableStateFlow(State())
    private val testDispatcher = StandardTestDispatcher()
    private val testSafeCoroutineExecutorScope =
        TestSafeCoroutineExecutorScope<State, Message, Action, Label>(
            stateFlow = stateFlow,
            context = testDispatcher,
        )
    private val sut = OnGreetClickHandler()

    @Test
    fun `greeting opens with the trimmed name`() =
        with(testSafeCoroutineExecutorScope) {
            stateFlow.value = State(name = "  Ada ")
            val expected = listOf(Label.OpenGreeting("Ada"))

            sut.runWith(Intent.OnGreetClick)
            testDispatcher.scheduler.runCurrent()

            assertEquals(expected, publishedLabels)
        }

    @Test
    fun `greeting does not open for a blank name`() =
        with(testSafeCoroutineExecutorScope) {
            stateFlow.value = State(name = "   ")
            val expected = emptyList<Label>()

            sut.runWith(Intent.OnGreetClick)
            testDispatcher.scheduler.runCurrent()

            assertEquals(expected, publishedLabels)
        }
}
