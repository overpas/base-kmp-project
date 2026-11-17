package by.overpas.template

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import by.overpas.template.App.Companion.appEntryPoint
import com.arkivanov.decompose.defaultComponentContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val rootEntryPoint = appEntryPoint.rootEntryPointFactory
            .create(defaultComponentContext())
        val rootComponent = rootEntryPoint.rootComponent
        setContent {
            App(
                rootComponent = rootComponent,
            )
        }
    }
}
