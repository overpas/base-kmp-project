package by.overpas.template.core.arch.mvi

import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutorScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

interface SafeCoroutineExecutorScope<State : Any, Message : Any, Action : Any, Label : Any> :
    CoroutineExecutorScope<State, Message, Action, Label> {

    val state: State

    suspend fun safeDispatch(message: Message)

    suspend fun safeForward(action: Action)

    suspend fun safePublish(label: Label)
}

internal class DispatcherSafeCoroutineExecutorScope<
    State : Any,
    Message : Any,
    Action : Any,
    Label : Any,
>(
    private val dispatcher: CoroutineDispatcher,
    private val delegate: CoroutineExecutorScope<State, Message, Action, Label>,
) : SafeCoroutineExecutorScope<State, Message, Action, Label>,
    CoroutineExecutorScope<State, Message, Action, Label> by delegate {

    override val state: State
        get() =
            delegate.state()

    override suspend fun safeDispatch(message: Message) =
        withContext(dispatcher) {
            delegate.dispatch(message)
        }

    override suspend fun safeForward(action: Action) =
        withContext(dispatcher) {
            delegate.forward(action)
        }

    override suspend fun safePublish(label: Label) =
        withContext(dispatcher) {
            delegate.publish(label)
        }
}

fun <State : Any, Message : Any, Action : Any, Label : Any>
    CoroutineExecutorScope<State, Message, Action, Label>.safeIn(
    dispatcher: CoroutineDispatcher,
): SafeCoroutineExecutorScope<State, Message, Action, Label> =
    DispatcherSafeCoroutineExecutorScope(
        dispatcher = dispatcher,
        delegate = this,
    )
