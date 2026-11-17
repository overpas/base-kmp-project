package by.overpas.template.feature.greeting.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performKeyInput
import by.overpas.template.feature.greeting.GreetingComponent
import com.atiurin.ultron.core.common.options.TextContainsOption
import com.atiurin.ultron.extensions.assertIsDisplayed
import com.atiurin.ultron.extensions.assertTextContains
import com.atiurin.ultron.extensions.click
import com.atiurin.ultron.extensions.perform

object GreetingPage {

    private val rootContainer = hasTestTag(GreetingTags.ROOT_CONTAINER)
    private val message = hasTestTag(GreetingTags.MESSAGE)
    private val backButton = hasTestTag(GreetingTags.BACK_BUTTON)

    @OptIn(ExperimentalTestApi::class)
    fun ComposeUiTest.setContent(component: GreetingComponent) {
        setContent {
            GreetingUi(
                component = component,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    fun clickBack() {
        backButton.click()
    }

    fun pressEscape() {
        rootContainer.perform { interaction ->
            interaction.performKeyInput {
                keyDown(Key.Escape)
                keyUp(Key.Escape)
            }
        }
    }

    fun assertRootContainerIsDisplayed() {
        rootContainer.assertIsDisplayed()
    }

    fun assertMessageContains(name: String) {
        message.assertTextContains(name, TextContainsOption(substring = true))
    }
}
