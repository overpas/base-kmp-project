package by.overpas.template.core.platform.state

import com.arkivanov.essenty.statekeeper.SerializableContainer
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File

internal interface SavedStateFileSerializer {

    fun serialize(
        container: SerializableContainer,
        file: File,
    )

    fun deserialize(file: File): SerializableContainer?
}

@ContributesBinding(AppScope::class)
@OptIn(ExperimentalSerializationApi::class)
internal class JsonSavedStateFileSerializer(private val json: Json) : SavedStateFileSerializer {

    override fun deserialize(file: File): SerializableContainer? =
        file.takeIf(File::exists)
            ?.run {
                inputStream().use { input ->
                    runCatching {
                        json.decodeFromStream(SerializableContainer.serializer(), input)
                    }.getOrNull()
                }
            }

    override fun serialize(
        container: SerializableContainer,
        file: File,
    ) {
        file.outputStream().use { output ->
            json.encodeToStream(
                serializer = SerializableContainer.serializer(),
                value = container,
                stream = output,
            )
        }
    }
}
