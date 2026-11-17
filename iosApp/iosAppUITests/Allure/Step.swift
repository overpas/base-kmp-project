import XCTest

private let stepScreenshotsVariable = "STEP_SCREENSHOTS"
private let stableScreenPollInterval: TimeInterval = 0.1
private let stableScreenTimeout: TimeInterval = 3

private let areStepScreenshotsEnabled = ProcessInfo.processInfo.environment[stepScreenshotsVariable] == "1"

func step<T>(_ name: String, _ block: () throws -> T) rethrows -> T {
    try XCTContext.runActivity(named: name) { activity in
        let result = try block()
        if areStepScreenshotsEnabled {
            let screenshot = XCTAttachment(screenshot: stableScreenshot())
            screenshot.name = name
            screenshot.lifetime = .keepAlways
            activity.add(screenshot)
        }
        return result
    }
}

private func stableScreenshot() -> XCUIScreenshot {
    let deadline = Date().addingTimeInterval(stableScreenTimeout)
    var previous = XCUIScreen.main.screenshot()
    while Date() < deadline {
        Thread.sleep(forTimeInterval: stableScreenPollInterval)
        let current = XCUIScreen.main.screenshot()
        if current.pngRepresentation == previous.pngRepresentation {
            return current
        }
        previous = current
    }
    return previous
}
