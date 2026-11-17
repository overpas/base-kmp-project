import XCTest

enum PhotoPicker {

    private static let timeoutSeconds: TimeInterval = 10

    static func pickNewestImage() {
        let photo = XCUIApplication().images.matching(identifier: "PXGGridLayout-Info").firstMatch
        XCTAssertTrue(photo.waitForExistence(timeout: timeoutSeconds), "The photo picker shows no photo")
        photo.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        XCTAssertTrue(photo.waitForNonExistence(timeout: timeoutSeconds), "The photo picker did not close")
    }
}
