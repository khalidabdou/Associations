package org.associations.project.database

import app.cash.sqldelight.db.SqlDriver
import java.io.InputStream
import java.io.OutputStream

expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
    fun exportDatabase(destinationPath: String)
    fun importDatabase(sourcePath: String)
    fun exportDatabaseToStream(out: OutputStream)
    fun importDatabaseFromStream(input: InputStream)
    fun clearDatabase()
}
