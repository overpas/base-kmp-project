import XCTest

final class PhotoPickerTest: XCTestCase {

    override func setUp() {
        super.setUp()
        continueAfterFailure = false
    }

    func testPickedImageIsTheSuppliedImage() throws {
        let id = try GalleryImage.add(named: "grid.png")
        addTeardownBlock { GalleryImage.remove(id: id) }
        let app = XCUIApplication()
        app.launch()

        PhotoPicker.pickNewestImage()

        let pickedImageSize = app.staticTexts["pickedImageSize"]
        XCTAssertTrue(pickedImageSize.waitForExistence(timeout: 10), "The host app shows no picked image")
        XCTAssertEqual(pickedImageSize.label, "900x1200")
    }
}
