import PhotosUI
import SwiftUI

@main
struct GalleryTestHostApp: App {

    var body: some Scene {
        WindowGroup {
            PickImageView()
        }
    }
}

struct PickImageView: View {

    @State private var isPickerPresented = true
    @State private var selection: PhotosPickerItem?
    @State private var pickedImageSize = ""

    var body: some View {
        VStack {
            if !pickedImageSize.isEmpty {
                Text(pickedImageSize)
                    .accessibilityIdentifier("pickedImageSize")
            }
        }
        .photosPicker(isPresented: $isPickerPresented, selection: $selection, matching: .images)
        .onChange(of: selection) { _, item in
            Task {
                guard
                    let data = try? await item?.loadTransferable(type: Data.self),
                    let image = UIImage(data: data)?.cgImage
                else {
                    return
                }
                pickedImageSize = "\(image.width)x\(image.height)"
            }
        }
    }
}
