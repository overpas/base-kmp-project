package by.overpas.template.core.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import by.overpas.template.core.arch.di.IoDispatcher
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File

@Inject
internal actual class DriverFactory(
    private val appDataPathProvider: AppDataPathProvider,
    private val schema: SqlSchema<QueryResult.Value<Unit>>,
    private val dbFileName: String,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    actual suspend fun create(): SqlDriver {
        val dbFile = File(appDataPathProvider(), dbFileName)
        val isNew = withContext(ioDispatcher) {
            dbFile.parentFile.mkdirs()
            !dbFile.exists()
        }
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.path}")
        if (isNew) {
            schema.create(driver)
        }
        return driver
    }
}
