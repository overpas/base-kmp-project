import Photos
import XCTest

private final class GalleryBundle {}

enum GalleryImage {

    private static let timeoutSeconds: TimeInterval = 10

    static func add(named name: String) throws -> String {
        let url = try XCTUnwrap(Bundle(for: GalleryBundle.self).url(forResource: name, withExtension: nil), "No \(name) in the test bundle")
        return try add(fileAt: url)
    }

    static func add(fileAt url: URL) throws -> String {
        let name = url.lastPathComponent
        allowFullAccess()
        var identifier: String?
        let added = XCTestExpectation(description: "Add \(name) to the gallery")
        PHPhotoLibrary.shared().performChanges({
            let request = PHAssetCreationRequest.forAsset()
            request.creationDate = Date()
            request.addResource(with: .photo, fileURL: url, options: nil)
            identifier = request.placeholderForCreatedAsset?.localIdentifier
        }, completionHandler: { success, error in
            XCTAssertTrue(success, "The gallery did not accept \(name): \(String(describing: error))")
            added.fulfill()
        })
        XCTAssertEqual(XCTWaiter().wait(for: [added], timeout: timeoutSeconds), .completed, "The gallery did not add \(name)")
        return try XCTUnwrap(identifier, "The gallery gave \(name) no identifier")
    }

    static func remove(id: String) {
        let assets = PHAsset.fetchAssets(withLocalIdentifiers: [id], options: nil)
        let removed = XCTestExpectation(description: "Remove \(id) from the gallery")
        PHPhotoLibrary.shared().performChanges({
            PHAssetChangeRequest.deleteAssets(assets)
        }, completionHandler: { success, error in
            XCTAssertTrue(success, "The gallery did not remove \(id): \(String(describing: error))")
            removed.fulfill()
        })
        SystemAlert.tap("Delete")
        XCTAssertEqual(XCTWaiter().wait(for: [removed], timeout: timeoutSeconds), .completed, "The gallery did not remove \(id)")
    }

    private static func allowFullAccess() {
        guard PHPhotoLibrary.authorizationStatus(for: .readWrite) != .authorized else {
            return
        }
        let authorized = XCTestExpectation(description: "Allow full access to the gallery")
        PHPhotoLibrary.requestAuthorization(for: .readWrite) { status in
            XCTAssertEqual(status, .authorized, "The gallery did not allow full access")
            authorized.fulfill()
        }
        SystemAlert.tap("Allow Full Access")
        XCTAssertEqual(XCTWaiter().wait(for: [authorized], timeout: timeoutSeconds), .completed, "The gallery access request did not end")
    }
}
