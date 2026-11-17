import XCTest

enum SystemAlert {

    private static let timeoutSeconds: TimeInterval = 10

    static func tap(_ label: String) {
        let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        let button = springboard.alerts.firstMatch.buttons[label]
        XCTAssertTrue(button.waitForExistence(timeout: timeoutSeconds), "The system alert has no button \(label)")
        button.tap()
    }
}
