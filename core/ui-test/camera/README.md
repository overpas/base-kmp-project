# core:ui-test:camera

Optional. Lets an Android end-to-end test choose the picture the emulator's back camera shows. A
test applies `BackCameraImage("<name>.png")` as a rule, or calls `BackCamera.show(...)`, with an
image from `androidApp/src/androidTest/camera/`. The camera relay in `scripts/camera-relay.sh`,
which `scripts/android-e2e-test.sh` starts, reads the request from logcat and loads the image into
the emulator scene over gRPC. Its own device tests check that a running camera sees the image.

Only Android has it: the iOS simulator has no camera, and desktop has none either.

It sits in a main source set because Gradle cannot share test code across projects.

## Removing it

1. Delete `core/ui-test/camera/` and `androidApp/src/androidTest/camera/`.
2. Delete `include(":core:ui-test:camera")` from `settings.gradle.kts`, its
   `androidTestImplementation` line from `androidApp/build.gradle.kts` and its path from
   `noTestCoveragePaths` in `build-logic/src/main/kotlin/code-coverage-aggregation.gradle.kts`.
3. Delete the `androidx-camera` version and the `androidx-camera-*` libraries from
   `gradle/libs.versions.toml`.
4. Delete `iosApp/UITestSupport/CameraPermissionAlert.swift` if no test uses it.

The e2e scripts skip the camera steps once the module is gone.
