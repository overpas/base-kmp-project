package by.overpas.template.feature.greeting

import kotlinx.coroutines.flow.MutableStateFlow

class FakeGreetingNavigator : GreetingNavigator {

    private val navigatedBack = MutableStateFlow(false)

    val isNavigateBackCalled: Boolean
        get() = navigatedBack.value

    override fun navigateBack() {
        navigatedBack.value = true
    }
}
