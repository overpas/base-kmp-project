package by.overpas.template.core.test

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent

context(testScope: TestScope)
inline fun <reified T> Flow<T>.collectCurrentOnBackground(): List<T> =
    collectOnBackground {
        runCurrent()
    }

context(testScope: TestScope)
inline fun <reified T> Flow<T>.collectOnBackground(block: TestScope.() -> Unit = {}): List<T> {
    val list = mutableListOf<T>()
    testScope.backgroundScope.launch {
        toCollection(list)
    }
    testScope.block()
    return list
}
