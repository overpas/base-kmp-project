import Shared
import XCTest

enum GreetingPage {

    private static let rootContainer = Node(GreetingTags.shared.ROOT_CONTAINER)
    private static let message = Node(GreetingTags.shared.MESSAGE)
    private static let backButton = Node(GreetingTags.shared.BACK_BUTTON)

    static func clickBack() {
        backButton.click()
    }

    static func assertRootContainerIsDisplayed() {
        rootContainer.assertIsDisplayed()
    }

    static func assertMessageContains(_ name: String) {
        message.assertTextContains(name)
    }
}
