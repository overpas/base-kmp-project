package by.overpas.template

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class RunOnUiThreadTest {

    @Test
    fun `the result of the block is returned`() {
        val expected = Result.success(42)

        val actual = runOnUiThread { 42 }

        assertEquals(expected, actual)
    }

    @Test
    fun `the failure of the block is returned`() {
        val expected = IllegalStateException("failed")

        val actual = runOnUiThread { throw expected }.exceptionOrNull()

        assertSame(expected, actual)
    }
}
