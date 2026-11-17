package by.overpas.template.feature.hello

import androidx.compose.runtime.Stable
import by.overpas.template.feature.hello.input.InputStore

@Stable
data class HelloState(
    val name: String = "",
    val isGreetEnabled: Boolean = false,
) {

    internal constructor(state: InputStore.State) : this(
        name = state.name,
        isGreetEnabled = state.isGreetEnabled,
    )
}
