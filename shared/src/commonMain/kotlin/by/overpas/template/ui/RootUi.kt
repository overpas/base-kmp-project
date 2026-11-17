package by.overpas.template.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import by.overpas.template.RootComponent
import by.overpas.template.feature.greeting.ui.GreetingUi
import by.overpas.template.feature.hello.ui.HelloUi
import com.arkivanov.decompose.extensions.compose.stack.Children

@Composable
fun RootUi(
    component: RootComponent,
    modifier: Modifier = Modifier,
) {
    Children(
        stack = component.stack,
        modifier = modifier.testTag(RootTags.CHILDREN),
    ) { createdChild ->
        when (val child = createdChild.instance) {
            is RootComponent.Child.Hello -> {
                HelloUi(
                    component = child.component,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            is RootComponent.Child.Greeting -> {
                GreetingUi(
                    component = child.component,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
