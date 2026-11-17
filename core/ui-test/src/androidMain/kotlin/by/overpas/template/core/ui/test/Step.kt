package by.overpas.template.core.ui.test

import android.os.SystemClock
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.test.platform.app.InstrumentationRegistry
import com.atiurin.ultron.allure.screenshot.AllureScreenshot
import com.atiurin.ultron.allure.step.step as allureStep

private const val STEP_SCREENSHOTS_ARGUMENT = "stepScreenshots"
private const val STABLE_SCREEN_POLL_MILLIS = 100L
private const val STABLE_SCREEN_TIMEOUT_MILLIS = 3_000L

private val areStepScreenshotsEnabled: Boolean =
    InstrumentationRegistry.getArguments().getString(STEP_SCREENSHOTS_ARGUMENT).toBoolean()

@ExperimentalTestApi
fun <T> ComposeUiTest.step(
    description: String,
    block: () -> T,
): T =
    allureStep(description) {
        block().also {
            if (areStepScreenshotsEnabled) {
                waitForIdle()
                awaitStableScreen()
                AllureScreenshot().takeAndAttach(description)
            }
        }
    }

private fun awaitStableScreen() {
    val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
    val deadline = SystemClock.uptimeMillis() + STABLE_SCREEN_TIMEOUT_MILLIS
    generateSequence {
        uiAutomation.takeScreenshot().also {
            SystemClock.sleep(
                STABLE_SCREEN_POLL_MILLIS,
            )
        }
    }
        .zipWithNext()
        .takeWhile { SystemClock.uptimeMillis() < deadline }
        .firstOrNull { (previous, current) -> previous.sameAs(current) }
}
