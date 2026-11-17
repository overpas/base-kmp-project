import XCTest

struct Node {

    private static let timeoutSeconds: TimeInterval = 10
    private static let maxScrollAttempts = 5

    private let predicate: NSPredicate
    private let name: String

    private var isOnScreen: Bool {
        element.exists && element.isHittable
    }

    private var element: XCUIElement {
        XCUIApplication().descendants(matching: .any).matching(predicate).firstMatch
    }

    init(_ identifier: String) {
        predicate = NSPredicate(format: "identifier == %@", identifier)
        name = identifier
    }

    init(identifierPrefix: String, labelContaining: String) {
        predicate = NSPredicate(format: "identifier BEGINSWITH %@ AND label CONTAINS %@", identifierPrefix, labelContaining)
        name = "\(identifierPrefix)* containing \(labelContaining)"
    }

    @discardableResult
    func scrollTo(in container: Node) -> Node {
        var attempts = 0
        while !isOnScreen && attempts < Node.maxScrollAttempts {
            container.element.swipeUp()
            attempts += 1
        }
        attempts = 0
        while !isOnScreen && attempts < Node.maxScrollAttempts {
            container.element.swipeDown()
            attempts += 1
        }
        return self
    }

    @discardableResult
    func click() -> Node {
        awaitHittable().tap()
        return self
    }

    @discardableResult
    func longPress() -> Node {
        awaitHittable().press(forDuration: 1)
        return self
    }

    @discardableResult
    func typeText(_ text: String) -> Node {
        awaitHittable().tap()
        _ = XCUIApplication().keyboards.firstMatch.waitForExistence(timeout: Node.timeoutSeconds)
        XCUIApplication().typeText(text)
        return self
    }

    @discardableResult
    func deleteLastCharacter() -> Node {
        awaitHittable().typeText(XCUIKeyboardKey.delete.rawValue)
        return self
    }

    @discardableResult
    func assertIsDisplayed() -> Node {
        awaitHittable()
        return self
    }

    @discardableResult
    func assertIsEnabled() -> Node {
        awaitMatching(NSPredicate(format: "enabled == true"), "never became enabled")
        return self
    }

    @discardableResult
    func assertIsNotEnabled() -> Node {
        awaitMatching(NSPredicate(format: "enabled == false"), "never became disabled")
        return self
    }

    @discardableResult
    func assertTextIsNotEmpty() -> Node {
        awaitMatching(NSPredicate(format: "label.length > 0"), "never showed a non-empty text")
        return self
    }

    @discardableResult
    func assertTextEquals(_ text: String) -> Node {
        awaitMatching(NSPredicate(format: "label == %@", text), "never showed the text \(text)")
        return self
    }

    @discardableResult
    func assertTextContains(_ text: String) -> Node {
        awaitMatching(NSPredicate(format: "label CONTAINS %@", text), "never showed a text containing \(text)")
        return self
    }

    @discardableResult
    func assertValueContains(_ text: String) -> Node {
        awaitMatching(NSPredicate(format: "value CONTAINS %@", text), "never held a value containing \(text)")
        return self
    }

    @discardableResult
    func assertDoesNotExist() -> Node {
        awaitMatching(NSPredicate(format: "exists == false"), "still exists")
        return self
    }

    @discardableResult
    private func awaitHittable() -> XCUIElement {
        awaitMatching(NSPredicate(format: "hittable == true"), "never became hittable")
        return element
    }

    private func awaitMatching(_ predicate: NSPredicate, _ failure: String) {
        let expectation = XCTNSPredicateExpectation(predicate: predicate, object: element)
        XCTAssertEqual(
            XCTWaiter().wait(for: [expectation], timeout: Node.timeoutSeconds),
            .completed,
            "Node with identifier \(name) \(failure)"
        )
    }
}
