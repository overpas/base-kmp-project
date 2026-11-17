package by.overpas.template.feature.hello

import by.overpas.template.core.arch.componentSharing
import by.overpas.template.core.arch.instanceScope
import by.overpas.template.feature.hello.input.InputSavedInput
import by.overpas.template.feature.hello.input.InputStore
import by.overpas.template.feature.hello.input.InputStoreFactory
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnCreate
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.stateFlow
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

interface HelloComponent {

    val state: StateFlow<HelloState>

    fun onNameChanged(name: String)

    fun onGreetClick()

    interface Factory {

        fun create(componentContext: ComponentContext): HelloComponent
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@AssistedInject
internal class DefaultHelloComponent(
    @Assisted componentContext: ComponentContext,
    storeFactory: InputStoreFactory,
    private val navigator: HelloNavigator,
) : HelloComponent,
    ComponentContext by componentContext {

    private val savedInput = stateKeeper.consume(SAVED_INPUT_KEY, InputSavedInput.serializer())

    private val store = instanceKeeper.getStore {
        storeFactory.create(savedInput)
    }

    override val state: StateFlow<HelloState> = store.stateFlow
        .map(::HelloState)
        .stateIn(scope = instanceScope, started = componentSharing, initialValue = HelloState())

    init {
        stateKeeper.register(SAVED_INPUT_KEY, InputSavedInput.serializer()) {
            InputSavedInput(store.state)
        }
        lifecycle.doOnCreate {
            store.labels
                .onEach(::handleLabel)
                .launchIn(instanceScope)
        }
    }

    private fun handleLabel(label: InputStore.Label) {
        when (label) {
            is InputStore.Label.OpenGreeting -> {
                navigator.openGreeting(label.name)
            }
        }
    }

    override fun onNameChanged(name: String) {
        store.accept(InputStore.Intent.OnNameChange(name))
    }

    override fun onGreetClick() {
        store.accept(InputStore.Intent.OnGreetClick)
    }

    private companion object {

        const val SAVED_INPUT_KEY = "HelloInputSavedInput"
    }

    @AssistedFactory
    internal abstract class Factory : HelloComponent.Factory {

        abstract override fun create(componentContext: ComponentContext): DefaultHelloComponent
    }
}

class PreviewHelloComponent : HelloComponent {

    override val state: StateFlow<HelloState> = MutableStateFlow(HelloState())

    override fun onNameChanged(name: String) =
        Unit

    override fun onGreetClick() =
        Unit
}
