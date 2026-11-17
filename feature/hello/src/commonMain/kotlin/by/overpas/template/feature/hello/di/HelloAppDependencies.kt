package by.overpas.template.feature.hello.di

import by.overpas.template.core.arch.di.MainDispatcher
import com.arkivanov.mvikotlin.core.store.StoreFactory
import kotlinx.coroutines.CoroutineDispatcher

interface HelloAppDependencies {

    val storeFactory: StoreFactory

    @MainDispatcher
    val mainDispatcher: CoroutineDispatcher
}
