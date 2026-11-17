package by.overpas.template.core.db

import app.cash.sqldelight.db.SqlDriver

internal expect class DriverFactory {

    suspend fun create(): SqlDriver
}
