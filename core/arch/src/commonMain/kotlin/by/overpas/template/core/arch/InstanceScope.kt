package by.overpas.template.core.arch

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext

internal class InstanceScope(override val coroutineContext: CoroutineContext) :
    CoroutineScope,
    InstanceKeeper.Instance {

    override fun onDestroy() {
        cancel("Instance is destroyed")
    }
}

val ComponentContext.instanceScope: CoroutineScope
    get() =
        instanceKeeper.getOrCreate {
            InstanceScope(SupervisorJob() + Dispatchers.Main.immediate)
        }
