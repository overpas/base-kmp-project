package by.overpas.template.core.ui.test.camera

import android.graphics.Bitmap
import java.io.File

private const val LUMA_SAMPLE_STEP = 8
private const val PNG_QUALITY = 100

internal class AnalysisFrame(
    val arrivalNanos: Long,
    val width: Int,
    val height: Int,
    val rotationDegrees: Int,
    val luminance: ByteArray,
) {
    val meanLuma: Int =
        luminance.indices.step(LUMA_SAMPLE_STEP).sumOf { luminance[it].toInt() and 0xFF } /
            (luminance.size / LUMA_SAMPLE_STEP)

    fun writePng(file: File) {
        val pixels = IntArray(luminance.size) { index ->
            val luma = luminance[index].toInt() and 0xFF
            (0xFF shl 24) or (luma shl 16) or (luma shl 8) or luma
        }
        file.outputStream().use { output ->
            Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
                .compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, output)
        }
    }
}
