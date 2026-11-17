import XCTest

final class GreetingE2eTest: XCTestCase {

    func testGreetingShowsTheEnteredNameAndGoesBack() {
        XCUIApplication().launch()

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
}
