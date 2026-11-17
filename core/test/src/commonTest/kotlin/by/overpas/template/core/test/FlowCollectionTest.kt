package by.overpas.template.core.test

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FlowCollectionTest {

    @Test
    fun `current values are collected`() =
        runTest {
            val sut = flowOf(1, 2, 3)
            val expected = listOf(1, 2, 3)

            val actual = sut.collectCurrentOnBackground()

            assertEquals(expected, actual)
        }

    @Test
    fun `values emitted in the block are collected`() =
        runTest {
            val sut = MutableStateFlow(0)
            val expected = listOf(0, 1)

            val actual = sut.collectOnBackground {
                runCurrent()
                sut.value = 1
                runCurrent()
            }

            assertEquals(expected, actual)
        }

    @Test
    fun `nothing is collected without running the scheduler`() =
        runTest {
            val sut = flowOf(1)
            val expected = emptyList<Int>()

            val actual = sut.collectOnBackground()

            assertEquals(expected, actual)
        }
}
