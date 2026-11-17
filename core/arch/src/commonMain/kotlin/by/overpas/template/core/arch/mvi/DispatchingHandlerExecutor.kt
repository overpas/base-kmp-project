package by.overpas.template.core.arch.mvi

import com.arkivanov.mvikotlin.core.store.Executor
import com.arkivanov.mvikotlin.core.utils.internal.InternalMviKotlinApi
import com.arkivanov.mvikotlin.core.utils.internal.atomic
import com.arkivanov.mvikotlin.core.utils.internal.initialize
import com.arkivanov.mvikotlin.core.utils.internal.requireValue
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutorScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlin.reflect.KClass

@OptIn(InternalMviKotlinApi::class)
class DispatchingHandlerExecutor<
    in Intent : Any,
    Action : Any,
    State : Any,
    Message : Any,
    Label : Any,
    >(
    private val executionScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    mainDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    private val intentHandlers:
    Map<KClass<*>, IntentHandler<State, Message, Action, Label, Intent>> = emptyMap(),
    private val actionHandlers:
    Map<KClass<*>, ActionHandler<State, Message, Action, Label, Action>> = emptyMap(),
) : Executor<Intent, Action, State, Message, Label>,
    SafeCoroutineExecutorScope<State, Message, Action, Label>,
    CoroutineExecutorScope<State, Message, Action, Label>,
    CoroutineScope by executionScope {

    private val safeCoroutineExecutorScope = safeIn(mainDispatcher)
    private val callbacks = atomic<Executor.Callbacks<State, Message, Action, Label>>()

    override val state: State
        get() = safeCoroutineExecutorScope.state

    override suspend fun safeDispatch(message: Message) {
        safeCoroutineExecutorScope.safeDispatch(message)
    }

    override suspend fun safeForward(action: Action) {
        safeCoroutineExecutorScope.safeForward(action)
    }

    override suspend fun safePublish(label: Label) {
        safeCoroutineExecutorScope.safePublish(label)
    }

    override fun init(callbacks: Executor.Callbacks<State, Message, Action, Label>) {
        this.callbacks.initialize(callbacks)
    }

    override fun state(): State =
        callbacks.requireValue().state

    override fun executeAction(action: Action) {
        actionHandlers[action::class]?.run { handle(action) }
    }

    override fun executeIntent(intent: Intent) {
        intentHandlers[intent::class]?.run { handle(intent) }
    }

    override fun dispose() {
        executionScope.cancel()
    }

    override fun dispatch(message: Message) {
        callbacks.requireValue().onMessage(message)
    }

    override fun forward(action: Action) {
        callbacks.requireValue().onAction(action)
    }

    override fun publish(label: Label) {
        callbacks.requireValue().onLabel(label)
    }
}
