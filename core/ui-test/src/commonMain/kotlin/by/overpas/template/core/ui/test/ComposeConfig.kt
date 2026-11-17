package by.overpas.template.core.ui.test

import com.atiurin.ultron.core.compose.config.UltronComposeConfig

fun configureCompose() {
    UltronComposeConfig.applyRecommended()
    UltronComposeConfig.params.operationTimeoutMs = UI_TEST_TIMEOUT_MILLIS
}
