package by.overpas.template.core.db.di

import app.cash.sqldelight.db.SqlDriver
import by.overpas.template.core.db.AppDatabase
import by.overpas.template.core.db.DriverFactory
import by.overpas.template.core.db.ItemQueries
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class ItemQueriesProvider(
    private val driverFactory: DriverFactory,
    private val ioDispatcher: CoroutineDispatcher,
) {

    private val mutex = Mutex()
    private val queries = CompletableDeferred<ItemQueries>()

    suspend fun get(): ItemQueries =
        mutex.withLock {
            if (!queries.isCompleted) {
                queries.complete(AppDatabase(driver()).itemQueries)
            }
            queries.await()
        }

    private suspend fun driver(): SqlDriver =
        withContext(ioDispatcher) { driverFactory.create() }
}
