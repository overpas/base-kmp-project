package by.overpas.template.core.arch.di

import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
interface ArchProviders {

    @Provides
    @SingleIn(AppScope::class)
    fun defaultStoreFactory(): StoreFactory =
        DefaultStoreFactory()
}
