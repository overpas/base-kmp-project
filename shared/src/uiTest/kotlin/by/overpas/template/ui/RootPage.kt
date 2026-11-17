package by.overpas.template.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import by.overpas.template.App
import by.overpas.template.RootComponent
import com.atiurin.ultron.extensions.assertIsDisplayed

object RootPage {

    private val children = hasTestTag(RootTags.CHILDREN)

    @OptIn(ExperimentalTestApi::class)
    fun ComposeUiTest.setContent(component: RootComponent) {
        setContent {
            App(
                rootComponent = component,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    fun assertChildrenDisplayed() {
        children.assertIsDisplayed()
    }
}
