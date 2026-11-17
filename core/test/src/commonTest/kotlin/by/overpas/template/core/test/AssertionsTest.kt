package by.overpas.template.core.test

import kotlin.test.Test
import kotlin.test.assertEquals

class AssertionsTest {

    @Test
    fun `block runs on the given value`() {
        val sut = { value: String, block: String.() -> Unit -> assertOn(value, block) }
        val received = mutableListOf<String>()
        val expected = listOf("value")

        sut("value") { received += this }

        assertEquals(expected, received)
    }
}
