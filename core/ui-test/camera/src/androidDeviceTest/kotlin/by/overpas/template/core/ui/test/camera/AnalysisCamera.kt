package by.overpas.template.core.ui.test.camera

import android.os.SystemClock
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.ExternalResource
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val DARK_IMAGE = "black.png"
private const val BRIGHT_IMAGE = "white.png"
private const val DARK_LUMA = 60
private const val BRIGHT_LUMA = 190
private const val NANOS_PER_MILLI = 1_000_000L
private const val SETTLE_NANOS = 1_000_000_000L
private const val RATE_NANOS = 3_000_000_000L

internal class AnalysisCamera : ExternalResource() {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val frames = AnalysisFrames()
    private val owner = TestLifecycleOwner()
    private lateinit var provider: ProcessCameraProvider
    private lateinit var analysisExecutor: ExecutorService

    override fun before() {
        analysisExecutor = Executors.newSingleThreadExecutor()
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        analysis.setAnalyzer(analysisExecutor, frames)
        provider = ProcessCameraProvider.getInstance(instrumentation.targetContext).get()
        instrumentation.runOnMainSync {
            owner.resume()
            provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, analysis)
        }
        frames.awaitFrame()
        BackCamera.show(BRIGHT_IMAGE)
        frames.awaitFramesUntil { it.isShowing(BRIGHT_IMAGE) }
        val rateStart = SystemClock.elapsedRealtimeNanos()
        val rateFrames = generateSequence { frames.awaitFrame() }
            .takeWhile { it.arrivalNanos - rateStart < RATE_NANOS }
            .toList()
        val first = rateFrames.first()
        TestLog.info(
            "frames ${first.width}x${first.height} rotation ${first.rotationDegrees}: " +
                "${rateFrames.size} in ${RATE_NANOS / NANOS_PER_MILLI} ms",
        )
    }

    override fun after() {
        instrumentation.runOnMainSync {
            provider.unbindAll()
            owner.destroy()
        }
        analysisExecutor.shutdown()
        BackCamera.show(BOOT_IMAGE)
    }

    fun oldFramesAfterSwitchTo(
        image: String,
        show: (String) -> Unit,
    ): Int {
        val requested = SystemClock.elapsedRealtimeNanos()
        show(image)
        val acknowledged = SystemClock.elapsedRealtimeNanos()
        val framesUntilSwitch = frames.awaitFramesUntil { it.isShowing(image) }
        val switched = framesUntilSwitch.last()
        val oldFramesAfterAcknowledgement = framesUntilSwitch
            .dropLast(1)
            .count { it.arrivalNanos > acknowledged }
        TestLog.info(
            "switch to $image: relay ${(acknowledged - requested) / NANOS_PER_MILLI} ms, " +
                "first new frame ${(switched.arrivalNanos - requested) / NANOS_PER_MILLI} ms " +
                "after request, " +
                "${(switched.arrivalNanos - acknowledged) / NANOS_PER_MILLI} ms after ack, " +
                "old frames after ack $oldFramesAfterAcknowledgement, " +
                "luma ${switched.meanLuma}",
        )
        return oldFramesAfterAcknowledgement
    }

    fun settledFrameAfter(
        image: String,
        show: (String) -> Unit,
    ): AnalysisFrame {
        show(image)
        val shown = SystemClock.elapsedRealtimeNanos()
        val frame = generateSequence { frames.awaitFrame() }
            .first { it.arrivalNanos - shown > SETTLE_NANOS }
        val directory = File(
            instrumentation.targetContext.filesDir,
            "back-camera-test",
        ).apply { mkdirs() }
        frame.writePng(File(directory, image))
        TestLog.info(
            "$image frame ${frame.width}x${frame.height} saved to ${directory.absolutePath}",
        )
        return frame
    }

    private fun AnalysisFrame.isShowing(image: String): Boolean =
        if (image == DARK_IMAGE) meanLuma < DARK_LUMA else meanLuma > BRIGHT_LUMA
}
