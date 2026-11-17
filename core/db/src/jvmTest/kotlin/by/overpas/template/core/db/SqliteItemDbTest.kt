package by.overpas.template.core.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SqliteItemDbTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Test
    fun `no names are stored at first`() =
        runTest(testDispatcher) {
            JdbcSqliteDriver(
                url = JdbcSqliteDriver.IN_MEMORY,
                properties = Properties(),
                schema = AppDatabase.Schema,
            ).use { driver ->
                val queries = ItemQueries(driver)
                val sut = SqliteItemDb(itemQueries = { queries }, ioDispatcher = testDispatcher)
                val expected = emptyList<String>()

                val actual = sut.observeNames().first()

                assertEquals(expected, actual)
            }
        }

    @Test
    fun `added names are observed in insertion order`() =
        runTest(testDispatcher) {
            JdbcSqliteDriver(
                url = JdbcSqliteDriver.IN_MEMORY,
                properties = Properties(),
                schema = AppDatabase.Schema,
            ).use { driver ->
                val queries = ItemQueries(driver)
                val sut = SqliteItemDb(itemQueries = { queries }, ioDispatcher = testDispatcher)
                val expected = listOf("Ada", "Grace")

                sut.add("Ada")
                sut.add("Grace")
                val actual = sut.observeNames().first()

                assertEquals(expected, actual)
            }
        }
}
