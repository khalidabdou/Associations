package org.associations.project.database

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
    fun exportDatabase(destinationPath: String)
    fun importDatabase(sourcePath: String)
    fun clearDatabase()
}
