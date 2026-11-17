package by.overpas.template.core.ui.test.camera

import android.util.Log

private const val TAG = "BackCameraTest"

internal object TestLog {

    fun info(message: String) {
        if (Log.isLoggable(TAG, Log.INFO)) {
            Log.i(TAG, message)
        }
    }
}
