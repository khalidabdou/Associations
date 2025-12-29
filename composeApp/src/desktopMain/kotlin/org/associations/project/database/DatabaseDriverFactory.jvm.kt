package org.associations.project.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

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

    actual fun clearDatabase() {
        val dbFile = File("AppDatabase.db")
        if (dbFile.exists()) {
            dbFile.delete()
        }
    }
}
