package by.overpas.template.core.db

import dev.zacsweers.metro.Inject
import java.util.Properties

private const val DATA_DIR_PROPERTY = "template.data.dir"
private const val APP_DIR_NAME = "Template"

@Inject
internal class AppDataPathProvider(private val properties: Properties) : () -> String {

    override fun invoke(): String {
        val osName = properties.getProperty("os.name").lowercase()
        val home = properties.getProperty("user.home")
        val dataDir = properties.getProperty(DATA_DIR_PROPERTY)
        return when {
            dataDir != null -> {
                dataDir
            }

            osName.startsWith("win") -> {
                """$home\AppData\Local\$APP_DIR_NAME"""
            }

            osName.startsWith("mac") -> {
                "$home/Library/Application Support/$APP_DIR_NAME"
            }

            else -> {
                "$home/.${APP_DIR_NAME.lowercase()}"
            }
        }
    }
}
