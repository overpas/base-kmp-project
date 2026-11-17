package by.overpas.template.e2e

import androidx.activity.ComponentActivity
import androidx.test.platform.app.InstrumentationRegistry

object SystemNavigation {

    fun pressBack(activity: ComponentActivity) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            activity.onBackPressedDispatcher.onBackPressed()
        }
    }
}
