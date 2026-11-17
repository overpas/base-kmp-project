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
internal class OnNameChangeHandler :
    IntentHandler<State, Message, Action, Label, Intent.OnNameChange> {

    override fun SafeCoroutineExecutorScope<State, Message, Action, Label>.handle(
        event: Intent.OnNameChange,
    ) {
        launch {
            safeDispatch(Message.NameChanged(event.name))
            safeForward(Action.CheckInput)
        }
    }
}
