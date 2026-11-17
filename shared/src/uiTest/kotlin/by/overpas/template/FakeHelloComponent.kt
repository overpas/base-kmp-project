package by.overpas.template

import by.overpas.template.feature.hello.HelloComponent
import by.overpas.template.feature.hello.HelloState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class FakeHelloComponent : HelloComponent {

    override val state: StateFlow<HelloState> = MutableStateFlow(HelloState())

    override fun onNameChanged(name: String) =
        Unit

    override fun onGreetClick() =
        Unit
}
