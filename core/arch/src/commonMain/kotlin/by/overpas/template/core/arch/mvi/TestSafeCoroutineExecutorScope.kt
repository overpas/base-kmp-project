package by.overpas.template.core.arch.mvi

import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutorScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class TestSafeCoroutineExecutorScope<State : Any, Message : Any, Action : Any, Label : Any>(
    stateFlow: MutableStateFlow<State>,
    private val context: CoroutineContext,
) : SafeCoroutineExecutorScope<State, Message, Action, Label>,
    CoroutineExecutorScope<State, Message, Action, Label> {

    private val store = RecordingStore<State, Message, Action, Label>(stateFlow)

    val dispatchedMessages: List<Message>
        get() = store.messages

    val forwardedActions: List<Action>
        get() = store.actions

    val publishedLabels: List<Label>
        get() = store.labels

    override val state: State
        get() =
            state()

    override val coroutineContext: CoroutineContext =
        context

    override fun state(): State =
        store.stateFlow.value

    override fun dispatch(message: Message) {
        store.messages += message
    }

    override fun forward(action: Action) {
        store.actions += action
    }

    override fun publish(label: Label) {
        store.labels += label
    }

    override suspend fun safeDispatch(message: Message) {
        withContext(context) {
            dispatch(message)
        }
    }

    override suspend fun safeForward(action: Action) {
        withContext(context) {
            forward(action)
        }
    }

    override suspend fun safePublish(label: Label) {
        withContext(context) {
            publish(label)
        }
    }

    fun <Event : Any> CoroutineHandler<State, Message, Action, Label, Event>.runWith(event: Event) {
        event.handleIn(this@TestSafeCoroutineExecutorScope)
    }
}
