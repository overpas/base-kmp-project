package by.overpas.template.core.arch.mvi

interface CoroutineHandler<State : Any, Message : Any, Action : Any, Label : Any, Event : Any> {

    fun SafeCoroutineExecutorScope<State, Message, Action, Label>.handle(event: Event)

    fun Event.handleIn(scope: SafeCoroutineExecutorScope<State, Message, Action, Label>) {
        scope.handle(this)
    }
}
