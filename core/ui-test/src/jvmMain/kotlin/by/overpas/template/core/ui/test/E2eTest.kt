package by.overpas.template.core.ui.test

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest

@ExperimentalTestApi
fun runE2eTest(
    content: @Composable () -> Unit,
    block: suspend DesktopComposeUiTest.() -> Unit,
) {
    runDesktopComposeUiTest {
        configureInstrumentedTest()
        setContent(content)
        block()
    }
}
