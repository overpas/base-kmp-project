package by.overpas.template

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performKeyInput
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.atiurin.ultron.extensions.assertIsFocused
import com.atiurin.ultron.extensions.perform

object EscapeAsBackPage {

    private val root = hasTestTag(EscapeAsBackTags.ROOT)

    @OptIn(ExperimentalTestApi::class)
    fun ComposeUiTest.setContent(backDispatcher: BackDispatcher) {
        setContent {
            EscapeAsBack(backDispatcher = backDispatcher) {}
        }
    }

    fun pressEscape() {
        root.perform { interaction ->
            interaction.performKeyInput {
                keyDown(Key.Escape)
                keyUp(Key.Escape)
            }
        }
    }

    fun assertIsFocused() {
        root.assertIsFocused()
    }
}
