package by.overpas.template.core.ui.test.gallery

import android.content.ContentUris
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

private const val PICK_TIMEOUT_SECONDS = 10L

internal class PhotoPickerTest {

    @get:Rule
    val galleryImage = GalleryImage("grid.png")

    @Test
    fun pickedImageIsTheSuppliedImage() {
        val sut = PhotoPicker
        val suppliedImageId = ContentUris.parseId(galleryImage.uri)
        val scenario = ActivityScenario.launch(PickImageActivity::class.java)
        lateinit var result: CompletableFuture<Uri?>
        scenario.onActivity { activity -> result = activity.pickedImage }

        sut.pickNewestImage()
        val pickedImageId = ContentUris.parseId(
            checkNotNull(result.get(PICK_TIMEOUT_SECONDS, TimeUnit.SECONDS)),
        )

        assertEquals(suppliedImageId, pickedImageId)
    }
}
