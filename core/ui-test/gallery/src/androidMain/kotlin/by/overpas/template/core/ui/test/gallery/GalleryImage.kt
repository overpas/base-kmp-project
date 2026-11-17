package by.overpas.template.core.ui.test.gallery

import android.net.Uri
import org.junit.rules.ExternalResource

class GalleryImage(private val image: String) : ExternalResource() {

    internal lateinit var uri: Uri
        private set

    override fun before() {
        uri = Gallery.add(image)
    }

    override fun after() {
        Gallery.remove(uri)
    }
}
