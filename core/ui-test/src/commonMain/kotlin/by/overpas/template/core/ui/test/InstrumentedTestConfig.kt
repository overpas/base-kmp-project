package by.overpas.template.core.ui.test

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import com.atiurin.ultron.core.compose.ComposeTestContainer
import com.atiurin.ultron.core.compose.UltronComposeTestEnvironment

@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.configureInstrumentedTest() {
    configureCompose()
    ComposeTestContainer.init(
        UltronComposeTestEnvironment(
            provider = this,
            density = density,
            mainClock = mainClock,
        ),
    )
}
