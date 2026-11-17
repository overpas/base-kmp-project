package by.overpas.template.core.ui.test.camera

import org.junit.rules.ExternalResource

class BackCameraImage(private val image: String) : ExternalResource() {

    override fun before() {
        BackCamera.show(image)
    }

    override fun after() {
        BackCamera.show(BOOT_IMAGE)
    }
}
