package org.associations.project.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

actual class DatabaseDriverFactory {
    private fun getDatabaseFile(): File {
        val osName = System.getProperty("os.name").lowercase()
        val appDir = when {
            osName.contains("win") -> {
                val appData = System.getenv("APPDATA") ?: System.getProperty("user.home")
                File(appData, "Associations")
            }
            osName.contains("mac") -> {
                File(System.getProperty("user.home"), "Library/Application Support/Associations")
            }
            else -> {
                File(System.getProperty("user.home"), ".associations")
            }
        }
        if (!appDir.exists()) {
            appDir.mkdirs()
        }

        val newDbFile = File(appDir, "AppDatabase.db")
        
        // Migrate legacy database and related sqlite files from CWD to safe folder if it exists
        val oldDbFile = File("AppDatabase.db")
        if (oldDbFile.exists() && !newDbFile.exists()) {
            try {
                oldDbFile.copyTo(newDbFile, overwrite = true)
                oldDbFile.renameTo(File(oldDbFile.parentFile, "AppDatabase.db.bak"))
                
                // Also migrate SQLite sidecar files (journal/wal/shm) if present
                listOf("-journal", "-wal", "-shm").forEach { suffix ->
                    val oldSidecar = File("AppDatabase.db$suffix")
                    if (oldSidecar.exists()) {
                        val newSidecar = File(appDir, "AppDatabase.db$suffix")
                        try {
                            oldSidecar.copyTo(newSidecar, overwrite = true)
                            oldSidecar.renameTo(File(oldSidecar.parentFile, "AppDatabase.db$suffix.bak"))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return newDbFile
    }

    actual fun createDriver(): SqlDriver {
        val dbFile = getDatabaseFile()
        val exists = dbFile.exists()
        val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")

        if (!exists) {
            AppDatabase.Schema.create(driver)
        }
        return driver
    }

    actual fun exportDatabase(destinationPath: String) {
        val dbFile = getDatabaseFile()
        if (dbFile.exists()) {
            // destinationPath is now the full path including filename
            dbFile.copyTo(File(destinationPath), overwrite = true)
        }
    }

    actual fun importDatabase(sourcePath: String) {
        val dbFile = getDatabaseFile()
        val sourceFile = File(sourcePath)
        if (sourceFile.exists()) {
            sourceFile.copyTo(dbFile, overwrite = true)
        }
    }

    actual fun exportDatabaseToStream(out: OutputStream) {
        val dbFile = getDatabaseFile()
        if (dbFile.exists()) {
            FileInputStream(dbFile).use { input -> input.copyTo(out) }
        } else {
            throw IllegalStateException("Database file not found")
        }
    }

    actual fun importDatabaseFromStream(input: InputStream) {
        val dbFile = getDatabaseFile()
        FileOutputStream(dbFile).use { output -> input.copyTo(output) }
    }

    actual fun clearDatabase() {
        val dbFile = getDatabaseFile()
        if (dbFile.exists()) {
            dbFile.delete()
        }
    }
}
