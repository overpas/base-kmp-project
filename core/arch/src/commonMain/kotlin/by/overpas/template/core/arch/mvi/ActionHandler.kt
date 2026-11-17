package by.overpas.template.core.arch.mvi

fun interface ActionHandler<
    State : Any,
    Message : Any,
    Action : Any,
    Label : Any,
    out TheAction : Any,
    > : CoroutineHandler<State, Message, Action, Label, @UnsafeVariance TheAction>
