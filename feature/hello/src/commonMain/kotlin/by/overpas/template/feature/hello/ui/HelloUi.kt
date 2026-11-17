package by.overpas.template.feature.hello.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.overpas.template.core.design.theme.Dimens
import by.overpas.template.core.strings.Res
import by.overpas.template.core.strings.hello_greet_button_label
import by.overpas.template.core.strings.hello_name_field_label
import by.overpas.template.core.strings.hello_title
import by.overpas.template.feature.hello.HelloComponent
import by.overpas.template.feature.hello.HelloState
import by.overpas.template.feature.hello.PreviewHelloComponent
import org.jetbrains.compose.resources.stringResource

@Preview
@Composable
fun HelloUi(
    modifier: Modifier = Modifier,
    component: HelloComponent = PreviewHelloComponent(),
) {
    val state by component.state.collectAsStateWithLifecycle()
    HelloContent(
        state = state,
        onNameChange = component::onNameChanged,
        onGreetClick = component::onGreetClick,
        modifier = modifier,
    )
}

@Composable
private fun HelloContent(
    state: HelloState,
    onNameChange: (String) -> Unit,
    onGreetClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Dimens.space4),
        modifier = modifier
            .testTag(HelloTags.ROOT_CONTAINER)
            .safeDrawingPadding()
            .padding(Dimens.space4),
    ) {
        Text(
            text = stringResource(Res.string.hello_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        OutlinedTextField(
            value = state.name,
            onValueChange = onNameChange,
            label = { Text(text = stringResource(Res.string.hello_name_field_label)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(HelloTags.NAME_FIELD),
        )
        Button(
            onClick = onGreetClick,
            enabled = state.isGreetEnabled,
            modifier = Modifier.testTag(HelloTags.GREET_BUTTON),
        ) {
            Text(text = stringResource(Res.string.hello_greet_button_label))
        }
    }
}
