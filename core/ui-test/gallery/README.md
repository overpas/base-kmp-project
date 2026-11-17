# core:ui-test:gallery

Optional. Lets an Android end-to-end test put an image in the device gallery and pick it in the
system photo picker. `GalleryImage("<asset>.png")` is a rule that adds an image from the test
assets before the test and removes it after; `PhotoPicker.pickNewestImage()` picks the newest photo
and confirms. Its own device tests check both against a small picker activity.

The iOS counterparts are `iosApp/UITestSupport/GalleryImage.swift` and `PhotoPicker.swift`. The
`GalleryTestHost` app and its `GalleryTestHostUITests` scheme test them, and
`scripts/ios-e2e.sh` runs that scheme.

It sits in a main source set because Gradle cannot share test code across projects.

## Removing it

1. Delete `core/ui-test/gallery/`.
2. Delete `include(":core:ui-test:gallery")` from `settings.gradle.kts`, its
   `androidTestImplementation` line from `androidApp/build.gradle.kts` and its path from
   `noTestCoveragePaths` in `build-logic/src/main/kotlin/code-coverage-aggregation.gradle.kts`.
3. Delete the `androidx-test-uiautomator` version and library from `gradle/libs.versions.toml`.
4. In Xcode, delete the `GalleryTestHost` and `GalleryTestHostUITests` targets and the
   `GalleryTestHostUITests` scheme, then delete `iosApp/GalleryTestHost/`,
   `iosApp/GalleryTestHostUITests/`, `iosApp/UITestSupport/GalleryImage.swift` and
   `iosApp/UITestSupport/PhotoPicker.swift`.

The e2e scripts skip the gallery steps once the module or the scheme is gone.
