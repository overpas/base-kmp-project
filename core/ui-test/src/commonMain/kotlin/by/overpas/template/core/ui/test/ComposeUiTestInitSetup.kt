package by.overpas.template.core.ui.test

import com.arkivanov.mvikotlin.core.utils.isAssertOnMainThreadEnabled

open class ComposeUiTestInitSetup {

    init {
        isAssertOnMainThreadEnabled = false
        configureCompose()
    }
}
