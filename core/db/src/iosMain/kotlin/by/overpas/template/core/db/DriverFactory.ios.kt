package by.overpas.template.core.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import dev.zacsweers.metro.Inject

@Inject
internal actual class DriverFactory(
    private val schema: SqlSchema<QueryResult.Value<Unit>>,
    private val dbFileName: String,
) {

    actual suspend fun create(): SqlDriver =
        NativeSqliteDriver(
            schema = schema,
            name = dbFileName,
        )
}
