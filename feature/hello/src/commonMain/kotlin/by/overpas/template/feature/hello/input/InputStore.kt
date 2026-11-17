package by.overpas.template.feature.hello.input

import by.overpas.template.feature.hello.di.HelloScope
import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

internal object InputStore {

    internal val reducer = Reducer<State, Message> { msg ->
        when (msg) {
            is Message.NameChanged -> copy(name = msg.name)
            is Message.InputChecked -> copy(isGreetEnabled = msg.isGreetEnabled)
        }
    }

    sealed interface Intent {

        data class OnNameChange(val name: String) : Intent

        data object OnGreetClick : Intent
    }

    internal sealed interface Action {

        data object CheckInput : Action
    }

    sealed interface Label {

        data class OpenGreeting(val name: String) : Label
    }

    internal sealed interface Message {

        data class NameChanged(val name: String) : Message

        data class InputChecked(val isGreetEnabled: Boolean) : Message
    }

    data class State(
        val name: String = "",
        val isGreetEnabled: Boolean = false,
    )

    @Inject
    @ContributesBinding(HelloScope::class)
    internal class Factory(
        private val storeFactory: StoreFactory,
        private val executorFactory: InputExecutorFactory,
    ) : InputStoreFactory {

        override fun create(savedInput: InputSavedInput?): Store<Intent, State, Label> =
            storeFactory.create(
                name = "HelloInputStore",
                initialState = savedInput?.toState() ?: State(),
                bootstrapper = SimpleBootstrapper(Action.CheckInput),
                executorFactory = executorFactory,
                reducer = reducer,
            )
    }
}
