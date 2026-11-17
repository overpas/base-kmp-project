package by.overpas.template.feature.hello

import by.overpas.template.feature.hello.di.HelloAppDependencies
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class FakeHelloAppDependencies(
    override val storeFactory: StoreFactory = DefaultStoreFactory(),
    override val mainDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : HelloAppDependencies
