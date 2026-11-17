package by.overpas.template.feature.greeting.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.overpas.template.core.design.theme.Dimens
import by.overpas.template.core.strings.Res
import by.overpas.template.core.strings.greeting_back_button_label
import by.overpas.template.core.strings.greeting_message
import by.overpas.template.feature.greeting.GreetingComponent
import by.overpas.template.feature.greeting.GreetingState
import by.overpas.template.feature.greeting.PreviewGreetingComponent
import org.jetbrains.compose.resources.stringResource

@Preview
@Composable
fun GreetingUi(
    modifier: Modifier = Modifier,
    component: GreetingComponent = PreviewGreetingComponent(),
) {
    val state by component.state.collectAsStateWithLifecycle()
    GreetingContent(
        state = state,
        onBackClick = component::onBackClick,
        modifier = modifier,
    )
}

@Composable
private fun GreetingContent(
    state: GreetingState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Dimens.space4),
        modifier = modifier
            .testTag(GreetingTags.ROOT_CONTAINER)
            .safeDrawingPadding()
            .padding(Dimens.space4),
    ) {
        Text(
            text = stringResource(Res.string.greeting_message, state.name),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.testTag(GreetingTags.MESSAGE),
        )
        OutlinedButton(
            onClick = onBackClick,
            modifier = Modifier.testTag(GreetingTags.BACK_BUTTON),
        ) {
            Text(text = stringResource(Res.string.greeting_back_button_label))
        }
    }
}
