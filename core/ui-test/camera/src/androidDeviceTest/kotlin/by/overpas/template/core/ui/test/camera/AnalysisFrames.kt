package by.overpas.template.core.ui.test.camera

import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

private const val FRAME_TIMEOUT_SECONDS = 10L
private const val FRAME_LIMIT = 90

internal class AnalysisFrames : ImageAnalysis.Analyzer {

    private val frames = LinkedBlockingQueue<AnalysisFrame>()

    override fun analyze(image: ImageProxy) {
        image.use { frame ->
            val plane = frame.planes[0]
            val buffer = plane.buffer
            val luminance = ByteArray(frame.width * frame.height)
            for (row in 0..<frame.height) {
                buffer.position(row * plane.rowStride)
                buffer.get(luminance, row * frame.width, frame.width)
            }
            frames.put(
                AnalysisFrame(
                    arrivalNanos = SystemClock.elapsedRealtimeNanos(),
                    width = frame.width,
                    height = frame.height,
                    rotationDegrees = frame.imageInfo.rotationDegrees,
                    luminance = luminance,
                ),
            )
        }
    }

    fun awaitFrame(): AnalysisFrame =
        checkNotNull(
            frames.poll(FRAME_TIMEOUT_SECONDS, TimeUnit.SECONDS),
        ) { "No camera frame in $FRAME_TIMEOUT_SECONDS s" }

    fun awaitFramesUntil(isWanted: (AnalysisFrame) -> Boolean): List<AnalysisFrame> {
        val awaited = buildList {
            do {
                add(awaitFrame())
            } while (!isWanted(last()) && size < FRAME_LIMIT)
        }
        check(
            isWanted(awaited.last()),
        ) { "No wanted frame in $FRAME_LIMIT frames, last luma ${awaited.last().meanLuma}" }
        return awaited
    }
}
