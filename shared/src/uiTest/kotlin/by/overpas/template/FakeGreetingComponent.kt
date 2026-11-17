package by.overpas.template

import by.overpas.template.feature.greeting.GreetingComponent
import by.overpas.template.feature.greeting.GreetingNavigator
import by.overpas.template.feature.greeting.GreetingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class FakeGreetingComponent(
    name: String,
    private val navigator: GreetingNavigator,
) : GreetingComponent {

    override val state: StateFlow<GreetingState> = MutableStateFlow(GreetingState(name = name))

    override fun onBackClick() {
        navigator.navigateBack()
    }
}
