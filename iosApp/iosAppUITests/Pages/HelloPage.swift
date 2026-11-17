import Shared
import XCTest

enum HelloPage {

    private static let rootContainer = Node(HelloTags.shared.ROOT_CONTAINER)
    private static let nameField = Node(HelloTags.shared.NAME_FIELD)
    private static let greetButton = Node(HelloTags.shared.GREET_BUTTON)

    static func typeName(_ name: String) {
        nameField.typeText(name)
    }

    static func clickGreet() {
        greetButton.click()
    }

    static func assertRootContainerIsDisplayed() {
        rootContainer.assertIsDisplayed()
    }

    static func assertNameContains(_ name: String) {
        nameField.assertValueContains(name)
    }

    static func assertGreetButtonIsEnabled() {
        greetButton.assertIsEnabled()
    }
}
