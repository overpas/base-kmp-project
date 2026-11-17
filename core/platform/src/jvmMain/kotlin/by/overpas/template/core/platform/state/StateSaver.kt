package by.overpas.template.core.platform.state

import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import java.io.File

interface StateSaver : StateKeeperDispatcher {

    operator fun invoke()
}

internal class FileStateSaver(
    private val file: File,
    private val savedStateFileSerializer: SavedStateFileSerializer,
) : StateSaver,
    StateKeeperDispatcher by StateKeeperDispatcher(savedStateFileSerializer.deserialize(file)) {

    override operator fun invoke() {
        savedStateFileSerializer.serialize(save(), file)
    }
}
