package by.overpas.template.feature.hello.input

import com.arkivanov.mvikotlin.core.store.Store

internal fun interface InputStoreFactory {

    fun create(
        savedInput: InputSavedInput?,
    ): Store<InputStore.Intent, InputStore.State, InputStore.Label>
}
