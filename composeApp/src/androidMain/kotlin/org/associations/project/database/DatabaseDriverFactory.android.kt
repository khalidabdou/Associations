package org.associations.project.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import java.io.File

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        val driver =
                AndroidSqliteDriver(
                        schema = AppDatabase.Schema,
                        context = context,
                        name = "AppDatabase.db",
                        callback =
                                object : AndroidSqliteDriver.Callback(AppDatabase.Schema) {
                                    override fun onOpen(
                                            db: androidx.sqlite.db.SupportSQLiteDatabase
                                    ) {
                                        super.onOpen(db)
                                        // ── Schema migrations for stale databases ──
                                        migrate(db)
                                    }
                                }
                )
        return driver
    }

    /** Apply all migrations to bring a stale database up to the current schema. */
    private fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        // Add isPenaltyApplied to Invoice if missing
        runSafe(db, "ALTER TABLE Invoice ADD COLUMN isPenaltyApplied INTEGER NOT NULL DEFAULT 0")

        // Create AppSettings table if missing
        runSafe(
                db,
                """
            CREATE TABLE IF NOT EXISTS AppSettings (
                id INTEGER NOT NULL PRIMARY KEY DEFAULT 1,
                lateFeeAmount REAL NOT NULL DEFAULT 5.0,
                monthlyFixedFee REAL NOT NULL DEFAULT 0.0,
                gracePeriodDays INTEGER NOT NULL DEFAULT 15,
                dueDateDays INTEGER NOT NULL DEFAULT 30,
                associationName TEXT NOT NULL DEFAULT 'Water Association',
                associationAddress TEXT NOT NULL DEFAULT '',
                associationPhone TEXT NOT NULL DEFAULT '',
                printFormat TEXT NOT NULL DEFAULT 'A4',
                logoPath TEXT DEFAULT NULL
            )
        """.trimIndent()
        )

        // Seed default settings row
        runSafe(
                db,
                """
            INSERT OR IGNORE INTO AppSettings(id, lateFeeAmount, monthlyFixedFee, gracePeriodDays, dueDateDays, associationName, associationAddress, associationPhone, printFormat, logoPath)
            VALUES (1, 5.0, 0.0, 15, 30, 'Water Association', '', '', 'A4', NULL)
        """.trimIndent()
        )

        // Create PricingTier table if missing
        runSafe(
                db,
                """
            CREATE TABLE IF NOT EXISTS PricingTier (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                minUsage INTEGER NOT NULL,
                maxUsage INTEGER NOT NULL,
                pricePerUnit REAL NOT NULL
            )
        """.trimIndent()
        )

        // Create MaintenanceTicket table if missing
        runSafe(
                db,
                """
            CREATE TABLE IF NOT EXISTS MaintenanceTicket (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                subscriberId INTEGER,
                issueType TEXT NOT NULL,
                description TEXT,
                status TEXT NOT NULL,
                reportedDate INTEGER NOT NULL,
                FOREIGN KEY (subscriberId) REFERENCES Subscriber(id)
            )
        """.trimIndent()
        )
    }

    private fun runSafe(db: androidx.sqlite.db.SupportSQLiteDatabase, sql: String) {
        try {
            db.execSQL(sql)
        } catch (_: Exception) {
            /* already exists */
        }
    }

    actual fun exportDatabase(destinationPath: String) {
        val dbFile = context.getDatabasePath("AppDatabase.db")
        if (dbFile.exists()) {
            dbFile.copyTo(File(destinationPath), overwrite = true)
        }
    }

    actual fun importDatabase(sourcePath: String) {
        val dbFile = context.getDatabasePath("AppDatabase.db")
        val sourceFile = File(sourcePath)
        if (sourceFile.exists()) {
            sourceFile.copyTo(dbFile, overwrite = true)
        }
    }

    actual fun clearDatabase() {
        val dbFile = context.getDatabasePath("AppDatabase.db")
        if (dbFile.exists()) {
            dbFile.delete()
        }
    }
}
