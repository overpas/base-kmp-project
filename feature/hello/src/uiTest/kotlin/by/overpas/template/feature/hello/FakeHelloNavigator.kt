package by.overpas.template.feature.hello

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeHelloNavigator : HelloNavigator {

    private val greetings = MutableStateFlow(emptyList<String>())

    val openedGreetings: List<String>
        get() = greetings.value

    override fun openGreeting(name: String) {
        greetings.update { opened -> opened + name }
    }
}
