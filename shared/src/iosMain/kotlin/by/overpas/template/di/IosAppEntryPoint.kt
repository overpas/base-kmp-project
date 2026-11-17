package by.overpas.template.di

import by.overpas.template.core.platform.di.IosPlatformGraph
import dev.zacsweers.metro.createGraph

fun createIosAppEntryPoint(): AppEntryPoint =
    createGraph<IosPlatformGraph>().createAppEntryPoint()
