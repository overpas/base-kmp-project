package by.overpas.template.core.db

import android.content.Context
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.zacsweers.metro.Inject

@Inject
internal actual class DriverFactory(
    private val context: Context,
    private val schema: SqlSchema<QueryResult.Value<Unit>>,
    private val dbFileName: String,
) {

    actual suspend fun create(): SqlDriver =
        AndroidSqliteDriver(
            schema = schema,
            context = context,
            name = dbFileName,
        )
}
