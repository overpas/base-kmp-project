package by.overpas.template.core.ui.test.gallery

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

private const val PHOTO_DESCRIPTION = "Photo taken on"
private const val DONE_TEXT = "Done"
private const val PICKER_TIMEOUT_MILLIS = 10_000L

object PhotoPicker {

    fun pickNewestImage() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val photo = By.descStartsWith(PHOTO_DESCRIPTION)
        val newestPhoto = checkNotNull(
            device.wait(Until.findObject(photo), PICKER_TIMEOUT_MILLIS),
        ) {
            "The photo picker shows no photo"
        }
        newestPhoto.click()
        val doneButton = checkNotNull(
            device.wait(Until.findObject(By.text(DONE_TEXT)), PICKER_TIMEOUT_MILLIS),
        ) {
            "The photo picker did not select the photo"
        }
        doneButton.click()
        check(
            device.wait(Until.gone(photo), PICKER_TIMEOUT_MILLIS),
        ) { "The photo picker did not close" }
    }
}
