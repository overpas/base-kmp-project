package by.overpas.template.core.platform.state

import by.overpas.template.core.test.assertOn
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonSavedStateFileSerializerTest {

    private val file = File("test_saved_state.dat")
    private val json = Json

    private val sut = JsonSavedStateFileSerializer(json)

    @Test
    fun `state is serialized and deserialized`() {
        val container = StateKeeperDispatcher().apply {
            register("key1", String.serializer()) { "value1" }
            register("key2", Boolean.serializer()) { false }
            register("key3", Double.serializer()) { 0.003 }
        }.save()

        sut.serialize(container, file)
        val restored = StateKeeperDispatcher(savedState = sut.deserialize(file))
        val actual = object {
            val value1 = restored.consume("key1", String.serializer())
            val isValue2 = restored.consume("key2", Boolean.serializer())
            val value3 = restored.consume("key3", Double.serializer())
        }

        assertOn(actual) {
            assertEquals("value1", value1)
            assertEquals(false, isValue2)
            assertEquals(0.003, value3)
        }
    }

    @AfterTest
    fun teardown() {
        file.takeIf(File::exists)
            ?.delete()
    }
}
