package by.overpas.template.core.ui.test.camera

import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID

private const val RELAY_TAG = "E2eCamera"
private const val RELAY_PROPERTY = "debug.e2e.camera"
private const val RELAY_POLL_MILLIS = 20L
private const val RELAY_TIMEOUT_MILLIS = 10_000L

internal const val BOOT_IMAGE = "default"

object BackCamera {

    fun show(image: String) {
        val request = UUID.randomUUID().toString()
        if (Log.isLoggable(RELAY_TAG, Log.INFO)) {
            Log.i(RELAY_TAG, "$request $image")
        }
        val deadline = SystemClock.uptimeMillis() + RELAY_TIMEOUT_MILLIS
        val reply = generateSequence { readRelayReply() }
            .onEach { SystemClock.sleep(RELAY_POLL_MILLIS) }
            .first { it.startsWith("$request:") || SystemClock.uptimeMillis() >= deadline }
        check(reply == "$request:shown") {
            val outcome = reply.takeIf { it.startsWith("$request:") }
                ?: "no reply, start the camera relay first"
            "The camera relay did not show $image: $outcome"
        }
    }

    private fun readRelayReply(): String =
        ParcelFileDescriptor.AutoCloseInputStream(
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                "getprop $RELAY_PROPERTY",
            ),
        ).bufferedReader().use { it.readText().trim() }
}
