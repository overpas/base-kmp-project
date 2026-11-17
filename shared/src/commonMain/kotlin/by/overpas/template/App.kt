package by.overpas.template

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import by.overpas.template.core.design.theme.AppTheme
import by.overpas.template.ui.RootUi

@Composable
fun App(
    rootComponent: RootComponent,
    modifier: Modifier = Modifier,
) {
    AppTheme {
        Surface(modifier = modifier.fillMaxSize()) {
            RootUi(
                component = rootComponent,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
