package by.overpas.template.core.db.di

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlSchema
import by.overpas.template.core.arch.di.IoDispatcher
import by.overpas.template.core.db.AppDatabase
import by.overpas.template.core.db.DriverFactory
import by.overpas.template.core.db.ItemDb
import by.overpas.template.core.db.SqliteItemDb
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphPrivate
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher

private const val DB_FILE_NAME = "app.db"

@BindingContainer
@ContributesTo(AppScope::class)
object DbProviders {

    @Provides
    @GraphPrivate
    @SingleIn(AppScope::class)
    internal fun schema(): SqlSchema<QueryResult.Value<Unit>> =
        AppDatabase.Schema

    @Provides
    @GraphPrivate
    internal fun fileName(): String =
        DB_FILE_NAME

    @Provides
    @GraphPrivate
    @SingleIn(AppScope::class)
    internal fun itemQueriesProvider(
        driverFactory: DriverFactory,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): ItemQueriesProvider =
        ItemQueriesProvider(
            driverFactory = driverFactory,
            ioDispatcher = ioDispatcher,
        )

    @Provides
    @SingleIn(AppScope::class)
    internal fun items(
        queriesProvider: ItemQueriesProvider,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): ItemDb =
        SqliteItemDb(
            itemQueries = queriesProvider::get,
            ioDispatcher = ioDispatcher,
        )
}
