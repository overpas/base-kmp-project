package by.overpas.template.core.arch.mvi

import com.arkivanov.mvikotlin.core.store.Executor

internal class FakeCallbacks(override val state: String = "state") :
    Executor.Callbacks<String, String, TestAction, String> {

    val messages = mutableListOf<String>()
    val actions = mutableListOf<TestAction>()
    val labels = mutableListOf<String>()

    override fun onMessage(message: String) {
        messages += message
    }

    override fun onAction(action: TestAction) {
        actions += action
    }

    override fun onLabel(label: String) {
        labels += label
    }
}
