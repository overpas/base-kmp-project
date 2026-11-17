@file:Suppress("UnusedImport")

package by.overpas.template.core.arch.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@BindingContainer
@ContributesTo(AppScope::class)
object DispatcherProviders {

    @MainDispatcher
    @Provides
    @SingleIn(AppScope::class)
    fun main(): CoroutineDispatcher =
        Dispatchers.Main.immediate

    @ComputationDispatcher
    @Provides
    @SingleIn(AppScope::class)
    fun computation(): CoroutineDispatcher =
        Dispatchers.Default

    @IoDispatcher
    @Provides
    @SingleIn(AppScope::class)
    fun inputOutput(): CoroutineDispatcher =
        Dispatchers.IO
}
