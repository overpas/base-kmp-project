package by.overpas.template.core.system.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import kotlin.time.Clock
import kotlin.time.Instant

@BindingContainer
@ContributesTo(AppScope::class)
object SystemProviders {

    @Provides
    fun currentTimeInstant(): Instant =
        Clock.System.now()
}
