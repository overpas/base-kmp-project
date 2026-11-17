package by.overpas.template

import androidx.compose.ui.test.ExperimentalTestApi
import by.overpas.template.core.ui.test.ComposeUiTestBase
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.atiurin.ultron.core.compose.runUltronUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class EscapeAsBackTest : ComposeUiTestBase() {

    private val backDispatcher = BackDispatcher()
    private val backPresses = mutableListOf<Unit>()
    private val sut = EscapeAsBackPage

    @Test
    fun `the handler takes the focus`() =
        runUltronUiTest {
            with(sut) { setContent(backDispatcher) }

            sut.assertIsFocused()
        }

    @Test
    fun `Escape goes back`() =
        runUltronUiTest {
            val expected = listOf(Unit)
            backDispatcher.register(BackCallback { backPresses += Unit })
            with(sut) { setContent(backDispatcher) }

            sut.pressEscape()

            assertEquals(expected, backPresses)
        }
}
