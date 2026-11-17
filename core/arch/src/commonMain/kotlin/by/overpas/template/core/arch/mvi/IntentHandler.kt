package by.overpas.template.core.arch.mvi

interface IntentHandler<State : Any, Message : Any, Action : Any, Label : Any, out Intent : Any> :
    CoroutineHandler<State, Message, Action, Label, @UnsafeVariance Intent>
