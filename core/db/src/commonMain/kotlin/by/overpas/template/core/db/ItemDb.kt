package by.overpas.template.core.db

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

interface ItemDb {

    fun observeNames(): Flow<List<String>>

    suspend fun add(name: String)
}

internal class SqliteItemDb(
    private val itemQueries: suspend () -> ItemQueries,
    private val ioDispatcher: CoroutineDispatcher,
) : ItemDb {

    override fun observeNames(): Flow<List<String>> =
        flow {
            emitAll(
                itemQueries()
                    .selectAllNames()
                    .asFlow()
                    .mapToList(ioDispatcher),
            )
        }

    override suspend fun add(name: String) {
        val queries = itemQueries()
        withContext(ioDispatcher) {
            queries.insert(name)
        }
    }
}
