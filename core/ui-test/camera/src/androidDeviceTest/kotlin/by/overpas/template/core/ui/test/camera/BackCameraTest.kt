package by.overpas.template.core.ui.test.camera

import android.Manifest
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

private const val SWITCHES = 12

internal class BackCameraTest {

    @get:Rule(order = 0)
    val cameraPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.CAMERA,
    )

    @get:Rule(order = 1)
    val camera = AnalysisCamera()

    @Test
    fun noOldFrameArrivesAfterTheSwitchIsAcknowledged() {
        val sut = BackCamera
        val images = List(SWITCHES) { if (it % 2 == 0) "black.png" else "white.png" }
        val noOldFrames = List(SWITCHES) { 0 }

        val oldFrameCounts = images.map { image -> camera.oldFramesAfterSwitchTo(image, sut::show) }

        assertEquals(noOldFrames, oldFrameCounts)
    }

    @Test
    fun analysisFrameKeepsItsSizeAfterTheSwitch() {
        val sut = BackCamera

        val frame = camera.settledFrameAfter("grid.png", sut::show)

        assertEquals("640x480", "${frame.width}x${frame.height}")
    }
}
