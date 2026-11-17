package by.overpas.template.di

import by.overpas.template.core.platform.PlatformApi
import dev.zacsweers.metro.createGraphFactory

interface AppEntryPoint {

    val rootEntryPointFactory: RootEntryPoint.Factory
}

fun PlatformApi.createAppEntryPoint(): AppEntryPoint =
    createGraphFactory<AppGraph.Factory>()
        .create(
            platformApi = this,
        )
