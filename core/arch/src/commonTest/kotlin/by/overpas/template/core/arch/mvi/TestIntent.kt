package by.overpas.template.core.arch.mvi

internal sealed interface TestIntent {

    data object First : TestIntent

    data object Second : TestIntent
}
