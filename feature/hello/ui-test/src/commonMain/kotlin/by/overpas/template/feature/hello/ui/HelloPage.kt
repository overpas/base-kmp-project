package by.overpas.template.feature.hello.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import by.overpas.template.feature.hello.HelloComponent
import com.atiurin.ultron.extensions.assertIsDisplayed
import com.atiurin.ultron.extensions.assertIsEnabled
import com.atiurin.ultron.extensions.assertIsNotEnabled
import com.atiurin.ultron.extensions.assertTextContains
import com.atiurin.ultron.extensions.click
import com.atiurin.ultron.extensions.typeText

object HelloPage {

    private val rootContainer = hasTestTag(HelloTags.ROOT_CONTAINER)
    private val nameField = hasTestTag(HelloTags.NAME_FIELD)
    private val greetButton = hasTestTag(HelloTags.GREET_BUTTON)

    @OptIn(ExperimentalTestApi::class)
    fun ComposeUiTest.setContent(component: HelloComponent) {
        setContent {
            HelloUi(
                component = component,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    fun typeName(name: String) {
        nameField.typeText(name)
    }

    fun clickGreet() {
        greetButton.click()
    }

    fun assertRootContainerIsDisplayed() {
        rootContainer.assertIsDisplayed()
    }

    fun assertNameContains(name: String) {
        nameField.assertTextContains(name)
    }

    fun assertGreetButtonIsEnabled() {
        greetButton.assertIsEnabled()
    }

    fun assertGreetButtonIsNotEnabled() {
        greetButton.assertIsNotEnabled()
    }
}
