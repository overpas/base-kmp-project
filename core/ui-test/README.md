# core:ui-test

Holds the base every Compose UI test extends, together with the Ultron configuration, the shared
matchers, and the entry point an end-to-end test uses to drive the installed application. Its single
Android implementation serves both the Robolectric host tests and the tests dexed onto a device.

It is a published module rather than a test fixture, because Gradle cannot share test code across
projects.

Two optional submodules extend it for Android end-to-end tests: `camera` drives the emulator's back
camera, and `gallery` drives the gallery and the system photo picker. Each one's README says how to
remove it.
