package by.overpas.template

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.PredictiveBackGestureOverlay
import com.arkivanov.essenty.backhandler.BackDispatcher

// Swift calls this factory as a type-like constructor in ContentView.swift, so it keeps PascalCase.
@Suppress("FunctionNaming")
@OptIn(ExperimentalDecomposeApi::class)
fun MainViewController(
    rootComponent: RootComponent,
    backDispatcher: BackDispatcher,
) =
    ComposeUIViewController {
        PredictiveBackGestureOverlay(
            backDispatcher = backDispatcher,
            backIcon = null,
            modifier = Modifier.fillMaxSize(),
        ) {
            App(
                rootComponent = rootComponent,
            )
        }
    }
