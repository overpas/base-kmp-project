package by.overpas.template.core.platform.di

import by.overpas.template.core.platform.PlatformApi
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph

@DependencyGraph(AppScope::class)
interface IosPlatformGraph : PlatformApi
