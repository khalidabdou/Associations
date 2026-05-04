package org.associations.project.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val dbFile = File("AppDatabase.db")
        val exists = dbFile.exists()
        val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")

        if (!exists) {
            AppDatabase.Schema.create(driver)
        }
        return driver
    }

    actual fun exportDatabase(destinationPath: String) {
        val dbFile = File("AppDatabase.db")
        if (dbFile.exists()) {
            // destinationPath is now the full path including filename
            dbFile.copyTo(File(destinationPath), overwrite = true)
        }
    }

    actual fun importDatabase(sourcePath: String) {
        val dbFile = File("AppDatabase.db")
        val sourceFile = File(sourcePath)
        if (sourceFile.exists()) {
            sourceFile.copyTo(dbFile, overwrite = true)
        }
    }

    actual fun exportDatabaseToStream(out: OutputStream) {
        val dbFile = File("AppDatabase.db")
        if (dbFile.exists()) {
            FileInputStream(dbFile).use { input -> input.copyTo(out) }
        } else {
            throw IllegalStateException("Database file not found")
        }
    }

    actual fun importDatabaseFromStream(input: InputStream) {
        val dbFile = File("AppDatabase.db")
        FileOutputStream(dbFile).use { output -> input.copyTo(output) }
    }

    actual fun clearDatabase() {
        val dbFile = File("AppDatabase.db")
        if (dbFile.exists()) {
            dbFile.delete()
        }
    }
}
