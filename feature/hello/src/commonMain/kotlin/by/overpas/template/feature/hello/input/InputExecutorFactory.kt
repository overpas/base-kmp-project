package by.overpas.template.feature.hello.input

import by.overpas.template.core.arch.di.MainDispatcher
import by.overpas.template.core.arch.mvi.ActionHandler
import by.overpas.template.core.arch.mvi.DispatchingHandlerExecutor
import by.overpas.template.core.arch.mvi.IntentHandler
import by.overpas.template.feature.hello.di.HelloScope
import by.overpas.template.feature.hello.input.InputStore.Action
import by.overpas.template.feature.hello.input.InputStore.Intent
import by.overpas.template.feature.hello.input.InputStore.Label
import by.overpas.template.feature.hello.input.InputStore.Message
import by.overpas.template.feature.hello.input.InputStore.State
import com.arkivanov.mvikotlin.core.store.Executor
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlin.reflect.KClass

@Inject
@SingleIn(HelloScope::class)
internal class InputExecutorFactory(
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
    private val intentHandlers:
    Map<KClass<*>, IntentHandler<State, Message, Action, Label, Intent>>,
    private val actionHandlers:
    Map<KClass<*>, ActionHandler<State, Message, Action, Label, Action>>,
) : () -> Executor<Intent, Action, State, Message, Label> {

    override fun invoke(): Executor<Intent, Action, State, Message, Label> =
        DispatchingHandlerExecutor(
            executionScope = CoroutineScope(mainDispatcher),
            mainDispatcher = mainDispatcher,
            intentHandlers = intentHandlers,
            actionHandlers = actionHandlers,
        )
}
