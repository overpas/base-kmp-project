package by.overpas.template.e2e

import androidx.compose.ui.test.hasTestTag
import by.overpas.template.EscapeAsBackTags
import com.atiurin.ultron.extensions.assertIsFocused

object EscapeAsBackPage {

    private val root = hasTestTag(EscapeAsBackTags.ROOT)

    fun assertIsFocused() {
        root.assertIsFocused()
    }
}
