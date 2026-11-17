import XCTest

enum CameraPermissionAlert {

    private static let timeoutSeconds: TimeInterval = 10

    static func allow() {
        tapButton(labelPrefix: "Allow")
    }

    static func deny() {
        tapButton(labelPrefix: "Don")
    }

    private static func tapButton(labelPrefix: String) {
        let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        let button = springboard.alerts.firstMatch.buttons
            .matching(NSPredicate(format: "label BEGINSWITH %@", labelPrefix))
            .firstMatch
        XCTAssertTrue(button.waitForExistence(timeout: timeoutSeconds), "The camera alert has no button \(labelPrefix)")
        button.tap()
    }
}
