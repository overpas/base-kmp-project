package by.overpas.template.core.db

import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals

class AppDataPathProviderTest {

    @Test
    fun `windows app data path`() {
        val properties = Properties().apply {
            setProperty("os.name", "Windows 10")
            setProperty("user.home", "home")
        }
        val expected = """home\AppData\Local\Template"""
        val sut = AppDataPathProvider(properties)

        val actual = sut.invoke()

        assertEquals(expected, actual)
    }

    @Test
    fun `macos app data path`() {
        val properties = Properties().apply {
            setProperty("os.name", "Mac OS X")
            setProperty("user.home", "/Users/username")
        }
        val expected = "/Users/username/Library/Application Support/Template"
        val sut = AppDataPathProvider(properties)

        val actual = sut.invoke()

        assertEquals(expected, actual)
    }

    @Test
    fun `default app data path`() {
        val properties = Properties().apply {
            setProperty("os.name", "Ubuntu 24.04")
            setProperty("user.home", "home")
        }
        val expected = "home/.template"
        val sut = AppDataPathProvider(properties)

        val actual = sut.invoke()

        assertEquals(expected, actual)
    }

    @Test
    fun `data dir property overrides os app data path`() {
        val properties = Properties().apply {
            setProperty("os.name", "Mac OS X")
            setProperty("user.home", "/Users/username")
            setProperty("template.data.dir", "data")
        }
        val expected = "data"
        val sut = AppDataPathProvider(properties)

        val actual = sut.invoke()

        assertEquals(expected, actual)
    }
}
