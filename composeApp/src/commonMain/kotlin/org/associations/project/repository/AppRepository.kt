package org.associations.project.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import org.associations.project.database.AppDatabase
import org.associations.project.database.Zone
import org.associations.project.database.Subscriber
import org.associations.project.database.GetAllSubscribers

class AppRepository(db: AppDatabase) {
    private val queries = db.appDatabaseQueries

    fun getZones(): Flow<List<Zone>> {
        return queries.getAllZones().asFlow().mapToList(Dispatchers.IO)
    }

    suspend fun insertZone(name: String, description: String?) {
        queries.insertZone(name, description)
    }

    fun getSubscribers(): Flow<List<GetAllSubscribers>> {
        return queries.getAllSubscribers().asFlow().mapToList(Dispatchers.IO)
    }

    suspend fun insertSubscriber(fullName: String, phone: String?, meterNumber: String, address: String?, zoneId: Long) {
        val createdAt = Clock.System.now().toEpochMilliseconds()
        queries.insertSubscriber(fullName, phone, meterNumber, address, zoneId, 1, createdAt)
    }
}
