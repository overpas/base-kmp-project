package by.overpas.template

import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import by.overpas.template.core.platform.di.JvmPlatformGraph
import by.overpas.template.core.strings.Res
import by.overpas.template.core.strings.app_name
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import dev.zacsweers.metro.createGraphFactory
import org.jetbrains.compose.resources.stringResource
import java.io.File
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask
import javax.swing.SwingUtilities

fun main() {
    val lifecycle = LifecycleRegistry()
    val backDispatcher = BackDispatcher()
    val platformApi = createGraphFactory<JvmPlatformGraph.Factory>()
        .create(
            properties = System.getProperties(),
            savedStateFile = File("saved_state.dat"),
        )
    val rootComponent = runOnUiThread {
        platformApi.createRootComponent(lifecycle, backDispatcher)
    }.getOrThrow()
    application {
        val windowState = rememberWindowState()
        Window(
            onCloseRequest = {
                platformApi.stateSaver()
                exitApplication()
            },
            state = windowState,
            title = stringResource(Res.string.app_name),
        ) {
            LifecycleController(
                lifecycleRegistry = lifecycle,
                windowState = windowState,
                windowInfo = LocalWindowInfo.current,
            )
            EscapeAsBack(backDispatcher = backDispatcher) {
                App(
                    rootComponent = rootComponent,
                )
            }
        }
    }
}

// Anything the block throws on the event dispatch thread has to reach the caller, errors included.
internal fun <T> runOnUiThread(block: () -> T): Result<T> {
    if (SwingUtilities.isEventDispatchThread()) {
        return runCatching(block)
    }
    val task = FutureTask(block)
    SwingUtilities.invokeAndWait(task)
    return try {
        Result.success(task.get())
    } catch (e: ExecutionException) {
        Result.failure(e.cause ?: e)
    }
}
