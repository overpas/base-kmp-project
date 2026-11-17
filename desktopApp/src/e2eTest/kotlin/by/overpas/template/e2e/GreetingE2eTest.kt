package by.overpas.template.e2e

import androidx.compose.ui.test.ExperimentalTestApi
import by.overpas.template.EscapeAsBackPage
import by.overpas.template.core.ui.test.runE2eTest
import by.overpas.template.core.ui.test.step
import by.overpas.template.feature.greeting.ui.GreetingPage
import by.overpas.template.feature.hello.ui.HelloPage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

@OptIn(ExperimentalTestApi::class)
class GreetingE2eTest {

    @Test
    fun greetingShowsTheEnteredNameAndGoesBack(@TempDir appDataDir: Path) =
        runE2eTest(content = { DesktopApp(appDataDir) }) {
            step("Enter a name") {
                HelloPage.typeName("Ada")
                HelloPage.assertGreetButtonIsEnabled()
            }

            step("Click Greet button") {
                HelloPage.clickGreet()
                GreetingPage.assertMessageContains("Ada")
            }

            step("Click Back button") {
                GreetingPage.clickBack()
                HelloPage.assertNameContains("Ada")
            }

            step("Assert Escape handler has focus") {
                EscapeAsBackPage.assertIsFocused()
            }
        }

    @Test
    fun greetingClosesOnEscape(@TempDir appDataDir: Path) =
        runE2eTest(content = { DesktopApp(appDataDir) }) {
            step("Enter a name") {
                HelloPage.typeName("Ada")
                HelloPage.assertGreetButtonIsEnabled()
            }

            step("Click Greet button") {
                HelloPage.clickGreet()
                GreetingPage.assertRootContainerIsDisplayed()
            }

            step("Press Escape") {
                GreetingPage.pressEscape()
                HelloPage.assertRootContainerIsDisplayed()
            }
        }
}
