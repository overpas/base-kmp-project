package by.overpas.template.core.platform

import by.overpas.template.core.platform.state.StateSaver
import java.util.Properties

actual interface PlatformApi {

    val properties: Properties

    val stateSaver: StateSaver
}
