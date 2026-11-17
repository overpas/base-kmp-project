package by.overpas.template.feature.hello.input

import kotlinx.serialization.Serializable

@Serializable
internal data class InputSavedInput(val name: String) {

    constructor(state: InputStore.State) : this(name = state.name)

    fun toState(): InputStore.State =
        InputStore.State(name = name)
}
