package by.overpas.template.core.platform.di

import by.overpas.template.core.platform.state.FileStateSaver
import by.overpas.template.core.platform.state.SavedStateFileSerializer
import by.overpas.template.core.platform.state.StateSaver
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.json.Json
import java.io.File

@BindingContainer
internal object JvmPlatformProviders {

    @Provides
    @SingleIn(AppScope::class)
    fun json(): Json =
        Json

    @Provides
    @SingleIn(AppScope::class)
    fun stateSaver(
        savedStateFile: File,
        savedStateFileSerializer: SavedStateFileSerializer,
    ): StateSaver =
        FileStateSaver(
            file = savedStateFile,
            savedStateFileSerializer = savedStateFileSerializer,
        )
}
