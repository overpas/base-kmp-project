package by.overpas.template.core.arch.mvi

import kotlinx.coroutines.flow.MutableStateFlow

internal data class RecordingStore<State : Any, Message : Any, Action : Any, Label : Any>(
    val stateFlow: MutableStateFlow<State>,
) {

    val messages = mutableListOf<Message>()
    val actions = mutableListOf<Action>()
    val labels = mutableListOf<Label>()
}
