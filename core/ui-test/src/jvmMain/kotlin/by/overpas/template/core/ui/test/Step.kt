package by.overpas.template.core.ui.test

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import io.qameta.allure.Allure
import io.qameta.allure.Allure.ThrowableRunnable
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

private const val SCREENSHOT_FORMAT = "png"
private const val SCREENSHOT_TYPE = "image/png"

@ExperimentalTestApi
fun <T> SkikoComposeUiTest.step(
    description: String,
    block: () -> T,
): T =
    Allure.step(
        description,
        ThrowableRunnable {
            block().also {
                waitForIdle()
                val screenshot = ByteArrayOutputStream()
                ImageIO.write(captureToImage().toAwtImage(), SCREENSHOT_FORMAT, screenshot)
                Allure.addAttachment(
                    description,
                    SCREENSHOT_TYPE,
                    screenshot.toByteArray().inputStream(),
                    SCREENSHOT_FORMAT,
                )
            }
        },
    )
