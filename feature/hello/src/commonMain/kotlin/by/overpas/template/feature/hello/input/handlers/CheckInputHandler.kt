package by.overpas.template.feature.hello.input.handlers

import by.overpas.template.core.arch.mvi.ActionHandler
import by.overpas.template.core.arch.mvi.SafeCoroutineExecutorScope
import by.overpas.template.feature.hello.input.InputStore.Action
import by.overpas.template.feature.hello.input.InputStore.Label
import by.overpas.template.feature.hello.input.InputStore.Message
import by.overpas.template.feature.hello.input.InputStore.State
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch

@Inject
internal class CheckInputHandler :
    ActionHandler<State, Message, Action, Label, Action.CheckInput> {

    override fun SafeCoroutineExecutorScope<State, Message, Action, Label>.handle(
        event: Action.CheckInput,
    ) {
        launch {
            safeDispatch(Message.InputChecked(isGreetEnabled = state.name.isNotBlank()))
        }
    }
}
