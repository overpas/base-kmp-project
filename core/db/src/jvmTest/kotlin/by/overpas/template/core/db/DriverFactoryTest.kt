package by.overpas.template.core.db

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class DriverFactoryTest {

    private val dataDir = Files.createTempDirectory("db").toFile()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val sut = DriverFactory(
        appDataPathProvider = AppDataPathProvider(
            Properties().apply {
                setProperty("os.name", "Linux")
                setProperty("user.home", "home")
                setProperty("template.data.dir", dataDir.path)
            },
        ),
        schema = AppDatabase.Schema,
        dbFileName = "test.db",
        ioDispatcher = testDispatcher,
    )

    @Test
    fun `a reopened database keeps its rows`() =
        runTest(testDispatcher) {
            val expected = listOf("Ada")
            sut.create().use { driver -> AppDatabase(driver).itemQueries.insert("Ada") }

            val actual = sut.create().use { driver ->
                AppDatabase(driver).itemQueries.selectAllNames().executeAsList()
            }

            assertEquals(expected, actual)
        }
}
