import Photos
import XCTest

final class GalleryImageTest: XCTestCase {

    override func setUp() {
        super.setUp()
        continueAfterFailure = false
    }

    func testSuppliedImageIsInTheGalleryAfterItIsAdded() throws {
        let id = try GalleryImage.add(named: "grid.png")
        addTeardownBlock { GalleryImage.remove(id: id) }

        let imagesAfterAdd = PHAsset.fetchAssets(withLocalIdentifiers: [id], options: nil).count

        XCTAssertEqual(imagesAfterAdd, 1)
    }

    func testSuppliedImageIsGoneAfterItIsRemoved() throws {
        let id = try GalleryImage.add(named: "grid.png")

        GalleryImage.remove(id: id)

        XCTAssertEqual(PHAsset.fetchAssets(withLocalIdentifiers: [id], options: nil).count, 0)
    }
}
