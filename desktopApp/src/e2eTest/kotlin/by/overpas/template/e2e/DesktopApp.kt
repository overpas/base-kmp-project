package by.overpas.template.e2e

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import by.overpas.template.App
import by.overpas.template.EscapeAsBack
import by.overpas.template.core.platform.di.JvmPlatformGraph
import by.overpas.template.createRootComponent
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import dev.zacsweers.metro.createGraphFactory
import java.nio.file.Path
import java.util.Properties

private const val DATA_DIR_PROPERTY = "template.data.dir"
private const val SAVED_STATE_FILE_NAME = "saved_state.dat"

@Composable
fun DesktopApp(appDataDir: Path) {
    val backDispatcher = remember(appDataDir) { BackDispatcher() }
    val rootComponent = remember(appDataDir) {
        val properties = Properties().apply {
            putAll(System.getProperties())
            setProperty(DATA_DIR_PROPERTY, appDataDir.toString())
        }
        val platformApi = createGraphFactory<JvmPlatformGraph.Factory>()
            .create(
                properties = properties,
                savedStateFile = appDataDir.resolve(SAVED_STATE_FILE_NAME).toFile(),
            )
        val lifecycle = LifecycleRegistry()
        platformApi.createRootComponent(lifecycle, backDispatcher).also { lifecycle.resume() }
    }
    EscapeAsBack(backDispatcher = backDispatcher) {
        App(rootComponent = rootComponent)
    }
}
