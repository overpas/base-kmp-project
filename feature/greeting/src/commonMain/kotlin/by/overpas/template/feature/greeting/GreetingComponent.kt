package by.overpas.template.feature.greeting

import com.arkivanov.decompose.ComponentContext
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface GreetingComponent {

    val state: StateFlow<GreetingState>

    fun onBackClick()

    interface Factory {

        fun create(
            componentContext: ComponentContext,
            name: String,
        ): GreetingComponent
    }
}

@AssistedInject
internal class DefaultGreetingComponent(
    @Assisted componentContext: ComponentContext,
    @Assisted name: String,
    private val navigator: GreetingNavigator,
) : GreetingComponent,
    ComponentContext by componentContext {

    override val state: StateFlow<GreetingState> = MutableStateFlow(GreetingState(name = name))

    override fun onBackClick() {
        navigator.navigateBack()
    }

    @AssistedFactory
    internal abstract class Factory : GreetingComponent.Factory {

        abstract override fun create(
            componentContext: ComponentContext,
            name: String,
        ): DefaultGreetingComponent
    }
}

class PreviewGreetingComponent : GreetingComponent {

    override val state: StateFlow<GreetingState> = MutableStateFlow(GreetingState(name = "Ada"))

    override fun onBackClick() =
        Unit
}
