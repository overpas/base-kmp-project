package by.overpas.template.core.system.time

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.createGraph
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

@DependencyGraph(AppScope::class)
internal interface CurrentTimeProviderTestGraph {

    fun inject(test: CurrentTimeProviderTest)
}

internal class CurrentTimeProviderTest {

    @Inject
    private lateinit var sut: () -> Instant

    @BeforeTest
    fun setup() {
        createGraph<CurrentTimeProviderTestGraph>()
            .inject(this)
    }

    @Test
    fun `current time provided`() {
        val actual = sut()

        assertTrue {
            val currentTimeInstant = Clock.System.now()
            actual in currentTimeInstant - 20.milliseconds..currentTimeInstant + 20.milliseconds
        }
    }

    @Test
    fun `different time provided`() {
        val time1 = sut()
        val threshold = time1 + 1.milliseconds
        while (Clock.System.now() < threshold) {
            // Spin until the clock passes the threshold,
            // so the second reading differs from the first.
        }
        val time2 = sut()

        assertTrue(time2 > time1)
    }
}
