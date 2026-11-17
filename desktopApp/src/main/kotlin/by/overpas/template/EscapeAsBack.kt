package by.overpas.template

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import com.arkivanov.essenty.backhandler.BackDispatcher

@Composable
fun EscapeAsBack(
    backDispatcher: BackDispatcher,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    var hasFocus by remember { mutableStateOf(false) }
    LaunchedEffect(focusRequester, hasFocus) {
        if (!hasFocus) {
            focusRequester.requestFocus()
        }
    }
    Box(
        modifier = modifier
            .testTag(EscapeAsBackTags.ROOT)
            .focusRequester(focusRequester)
            .onFocusChanged { hasFocus = it.hasFocus }
            .focusable()
            .onPreviewKeyEvent { event ->
                event.type == KeyEventType.KeyDown && event.key == Key.Escape &&
                    backDispatcher.back()
            },
    ) {
        content()
    }
}
