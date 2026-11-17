package by.overpas.template.core.ui.test

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.AndroidComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runAndroidComposeUiTest

@ExperimentalTestApi
inline fun <reified A : ComponentActivity> runE2eTest(
    noinline block: suspend AndroidComposeUiTest<A>.() -> Unit,
) {
    runAndroidComposeUiTest<A> {
        configureInstrumentedTest()
        configureAllure()
        block()
    }
}
