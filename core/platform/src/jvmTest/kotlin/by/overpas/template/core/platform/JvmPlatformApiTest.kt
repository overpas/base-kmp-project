package by.overpas.template.core.platform

import by.overpas.template.core.platform.di.JvmPlatformGraph
import dev.zacsweers.metro.createGraphFactory
import java.io.File
import java.util.Properties
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class JvmPlatformApiTest {

    private val props = Properties()
    private val savedStateFile = File("test_saved_state.dat")

    private lateinit var sut: JvmPlatformGraph

    @BeforeTest
    fun setup() {
        sut = createGraphFactory<JvmPlatformGraph.Factory>()
            .create(
                properties = props,
                savedStateFile = savedStateFile,
            )
    }

    @Test
    fun `properties received`() {
        val actual = sut.properties

        assertEquals(props, actual)
    }

    @Test
    fun `StateSaver received`() {
        val actual = sut.stateSaver

        assertNotNull(actual)
    }
}
