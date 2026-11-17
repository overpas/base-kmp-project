package by.overpas.template.feature.hello.input.handlers

import by.overpas.template.core.arch.mvi.IntentHandler
import by.overpas.template.core.arch.mvi.SafeCoroutineExecutorScope
import by.overpas.template.feature.hello.input.InputStore.Action
import by.overpas.template.feature.hello.input.InputStore.Intent
import by.overpas.template.feature.hello.input.InputStore.Label
import by.overpas.template.feature.hello.input.InputStore.Message
import by.overpas.template.feature.hello.input.InputStore.State
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch

@Inject
internal class OnGreetClickHandler :
    IntentHandler<State, Message, Action, Label, Intent.OnGreetClick> {

    override fun SafeCoroutineExecutorScope<State, Message, Action, Label>.handle(
        event: Intent.OnGreetClick,
    ) {
        launch {
            val name = state.name.trim()
            if (name.isNotEmpty()) {
                safePublish(Label.OpenGreeting(name))
            }
        }
    }
}
