package by.overpas.template.core.ui.test.gallery

import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.Description
import org.junit.runners.model.Statement

internal class GalleryImageTest {

    @Test
    fun suppliedImageIsInTheGalleryDuringTheRule() {
        val sut = GalleryImage("grid.png")
        val resolver = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
        var imagesDuringTheRule = -1

        sut.apply(
            object : Statement() {
                override fun evaluate() {
                    imagesDuringTheRule = checkNotNull(
                        resolver.query(
                            Gallery.images,
                            arrayOf(MediaStore.Images.Media._ID),
                            "${MediaStore.Images.Media.DISPLAY_NAME} = ?",
                            arrayOf("grid.png"),
                            null,
                        )?.use { it.count },
                    )
                }
            },
            Description.EMPTY,
        ).evaluate()

        assertEquals(1, imagesDuringTheRule)
    }

    @Test
    fun suppliedImageIsGoneAfterTheRule() {
        val sut = GalleryImage("grid.png")
        val resolver = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver

        sut.apply(
            object : Statement() {
                override fun evaluate() =
                    Unit
            },
            Description.EMPTY,
        ).evaluate()
        val imagesAfterTheRule = checkNotNull(
            resolver.query(
                Gallery.images,
                arrayOf(MediaStore.Images.Media._ID),
                "${MediaStore.Images.Media.DISPLAY_NAME} = ?",
                arrayOf("grid.png"),
                null,
            )?.use { it.count },
        )

        assertEquals(0, imagesAfterTheRule)
    }
}
