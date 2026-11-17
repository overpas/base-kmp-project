package by.overpas.template.core.ui.test.gallery

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry

private const val GALLERY_DIRECTORY = "Pictures/template-e2e"
private const val IMAGE_MIME_TYPE = "image/png"
private const val UNSUPPORTED_ANDROID_MESSAGE = "The gallery needs Android 10 or later"

object Gallery {

    val images: Uri
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            error(UNSUPPORTED_ANDROID_MESSAGE)
        }

    fun add(image: String): Uri {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { UNSUPPORTED_ANDROID_MESSAGE }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val resolver = instrumentation.targetContext.contentResolver
        val pending = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, image)
            put(MediaStore.Images.Media.MIME_TYPE, IMAGE_MIME_TYPE)
            put(MediaStore.Images.Media.RELATIVE_PATH, GALLERY_DIRECTORY)
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = checkNotNull(
            resolver.insert(images, pending),
        ) { "The gallery did not accept $image" }
        val written = resolver.openOutputStream(uri)?.use { output ->
            instrumentation.context.assets.open(image).use { it.copyTo(output) }
        }
        checkNotNull(written) { "The gallery cannot write $image" }
        resolver.update(
            uri,
            ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
            null,
            null,
        )
        return uri
    }

    fun remove(uri: Uri) {
        InstrumentationRegistry.getInstrumentation().targetContext.contentResolver.delete(
            uri,
            null,
            null,
        )
    }
}
