package by.overpas.template.core.arch.mvi

internal sealed interface TestAction {

    data object First : TestAction

    data object Second : TestAction
}
