package by.overpas.template.core.ui.test.camera

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

internal class TestLifecycleOwner : LifecycleOwner {

    override val lifecycle: Lifecycle
        field = LifecycleRegistry(this)

    fun resume() {
        lifecycle.currentState = Lifecycle.State.RESUMED
    }

    fun destroy() {
        lifecycle.currentState = Lifecycle.State.DESTROYED
    }
}
