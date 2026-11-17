package by.overpas.template.e2e

import androidx.compose.ui.test.ExperimentalTestApi
import by.overpas.template.MainActivity
import by.overpas.template.core.ui.test.runE2eTest
import by.overpas.template.core.ui.test.step
import by.overpas.template.feature.greeting.ui.GreetingPage
import by.overpas.template.feature.hello.ui.HelloPage
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class GreetingE2eTest {

    @Test
    fun greetingShowsTheEnteredNameAndGoesBack() =
        runE2eTest<MainActivity> {
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
        }

    @Test
    fun greetingClosesOnSystemBack() =
        runE2eTest<MainActivity> {
            step("Enter a name") {
                HelloPage.typeName("Ada")
                HelloPage.assertGreetButtonIsEnabled()
            }

            step("Click Greet button") {
                HelloPage.clickGreet()
                GreetingPage.assertRootContainerIsDisplayed()
            }

            step("Press system back") {
                SystemNavigation.pressBack(requireNotNull(activity))
                HelloPage.assertRootContainerIsDisplayed()
            }
        }
}
