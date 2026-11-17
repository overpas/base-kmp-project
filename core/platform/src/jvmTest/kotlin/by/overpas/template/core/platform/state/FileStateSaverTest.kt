package by.overpas.template.core.platform.state

import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue

internal class FileStateSaverTest {

    private val json = Json
    private val savedStateFile = File("test_saved_state.dat")
    private val savedStateFileSerializer = JsonSavedStateFileSerializer(json)

    private val sut = FileStateSaver(
        file = savedStateFile,
        savedStateFileSerializer = savedStateFileSerializer,
    )

    @Test
    fun `state is saved to file`() {
        sut.apply {
            register("key1", String.serializer()) { "value1" }
            register("key2", Boolean.serializer()) { false }
            register("key3", Double.serializer()) { 0.003 }
        }

        sut.invoke()
        val actual = savedStateFile.readText()

        assertTrue { actual.isNotEmpty() }
    }

    @AfterTest
    fun teardown() {
        savedStateFile.takeIf(File::exists)
            ?.delete()
    }
}
