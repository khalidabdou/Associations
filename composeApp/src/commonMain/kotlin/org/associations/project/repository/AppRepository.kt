package org.associations.project.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import org.associations.project.database.*

class AppRepository(db: AppDatabase) {
    private val queries = db.appDatabaseQueries

    suspend fun initializeDatabase() {
        try {
            val existingZones = queries.getAllZones().executeAsList()
            if (existingZones.isEmpty()) {
                queries.insertZone("المنطقة أ", "المنطقة السكنية الرئيسية")
                queries.insertZone("المنطقة ب", "المنطقة التجارية")
                queries.insertZone("المنطقة ج", "المنطقة الصناعية")
            }
            val existingTiers = queries.getAllPricingTiers().executeAsList()
            if (existingTiers.isEmpty()) {
                queries.insertPricingTier(0, 5, 5.0)
                queries.insertPricingTier(6, 15, 8.0)
                queries.insertPricingTier(16, 100, 12.0)
            }
        } catch (e: Exception) {
            println("Database initialization error: ${e.message}")
        }
    }

    // ===== Zone Operations =====
    fun getZones(): Flow<List<Zone>> {
        return queries.getAllZones().asFlow().mapToList(Dispatchers.IO)
    }

    suspend fun insertZone(name: String, description: String?) {
        queries.insertZone(name, description)
    }

    suspend fun updateZone(id: Long, name: String, description: String?) {
        queries.updateZone(name, description, id)
    }

    suspend fun deleteZone(id: Long) {
        queries.deleteZone(id)
    }

    // ===== Subscriber Operations =====
    fun getSubscribers(): Flow<List<GetAllSubscribers>> {
        return queries.getAllSubscribers().asFlow().mapToList(Dispatchers.IO)
    }

    fun getSubscribersByZone(zoneId: Long): Flow<List<GetSubscribersByZone>> {
        return queries.getSubscribersByZone(zoneId).asFlow().mapToList(Dispatchers.IO)
    }

    fun searchSubscribers(query: String): Flow<List<SearchSubscribers>> {
        return queries.searchSubscribers(query, query, query).asFlow().mapToList(Dispatchers.IO)
    }

    fun getSubscriberById(id: Long): Flow<Subscriber?> {
        return queries.getSubscriberById(id).asFlow().mapToOneOrNull(Dispatchers.IO)
    }

    suspend fun insertSubscriber(fullName: String, phone: String?, meterNumber: String, address: String?, zoneId: Long) {
        val createdAt = Clock.System.now().toEpochMilliseconds()
        queries.insertSubscriber(fullName, phone, meterNumber, address, zoneId, 1, createdAt)
    }

    suspend fun updateSubscriber(id: Long, fullName: String, phone: String?, meterNumber: String, address: String?, zoneId: Long, isActive: Long) {
        queries.updateSubscriber(fullName, phone, meterNumber, address, zoneId, isActive, id)
    }

    suspend fun deleteSubscriber(id: Long) {
        queries.deleteSubscriber(id)
    }

    fun getSubscriberCount(): Flow<Long> {
        return queries.getSubscriberCount().asFlow().mapToOneOrNull(Dispatchers.IO).map { it ?: 0L }
    }

    // ===== Invoice Operations =====
    fun getAllInvoices(): Flow<List<GetAllInvoices>> {
        return queries.getAllInvoices().asFlow().mapToList(Dispatchers.IO)
    }

    fun getUnpaidInvoices(): Flow<List<GetUnpaidInvoices>> {
        return queries.getUnpaidInvoices().asFlow().mapToList(Dispatchers.IO)
    }

    fun getPaidInvoices(): Flow<List<GetPaidInvoices>> {
        return queries.getPaidInvoices().asFlow().mapToList(Dispatchers.IO)
    }

    fun getInvoicesBySubscriber(subscriberId: Long): Flow<List<Invoice>> {
        return queries.getInvoicesBySubscriber(subscriberId).asFlow().mapToList(Dispatchers.IO)
    }

    fun getInvoiceById(id: Long): Flow<GetInvoiceById?> {
        return queries.getInvoiceById(id).asFlow().mapToOneOrNull(Dispatchers.IO)
    }

    suspend fun insertInvoice(
        subscriberId: Long,
        previousReading: Long,
        currentReading: Long,
        consumption: Long,
        totalAmount: Double,
        status: String,
        issueDate: Long,
        dueDate: Long
    ) {
        queries.insertInvoice(subscriberId, previousReading, currentReading, consumption, totalAmount, status, issueDate, dueDate)
    }

    suspend fun updateInvoiceStatus(id: Long, status: String) {
        queries.updateInvoiceStatus(status, id)
    }

    suspend fun deleteInvoice(id: Long) {
        queries.deleteInvoice(id)
    }

    fun getTotalUnpaidAmount(): Flow<Double> {
        return queries.getTotalUnpaidAmount().asFlow().mapToOneOrNull(Dispatchers.IO).map { it ?: 0.0 }
    }

    fun getTotalConsumption(): Flow<Long> {
        return queries.getTotalConsumption().asFlow().mapToOneOrNull(Dispatchers.IO).map { it ?: 0L }
    }

    // ===== Transaction Operations =====
    fun getAllTransactions(): Flow<List<TransactionTable>> {
        return queries.getAllTransactions().asFlow().mapToList(Dispatchers.IO)
    }

    fun getTransactionsByType(type: String): Flow<List<TransactionTable>> {
        return queries.getTransactionsByType(type).asFlow().mapToList(Dispatchers.IO)
    }

    suspend fun insertTransaction(type: String, category: String, amount: Double, description: String?, date: Long) {
        queries.insertTransaction(type, category, amount, description, date)
    }

    suspend fun deleteTransaction(id: Long) {
        queries.deleteTransaction(id)
    }

    fun getTotalIncome(): Flow<Double> {
        return queries.getTotalIncome().asFlow().mapToOneOrNull(Dispatchers.IO).map { it ?: 0.0 }
    }

    fun getTotalExpenses(): Flow<Double> {
        return queries.getTotalExpenses().asFlow().mapToOneOrNull(Dispatchers.IO).map { it ?: 0.0 }
    }

    fun getBalance(): Flow<Double> {
        return queries.getBalance().asFlow().mapToOneOrNull(Dispatchers.IO).map { it ?: 0.0 }
    }

    // ===== Maintenance Ticket Operations =====
    fun getAllMaintenanceTickets(): Flow<List<GetAllMaintenanceTickets>> {
        return queries.getAllMaintenanceTickets().asFlow().mapToList(Dispatchers.IO)
    }

    fun getTicketsByStatus(status: String): Flow<List<GetTicketsByStatus>> {
        return queries.getTicketsByStatus(status).asFlow().mapToList(Dispatchers.IO)
    }

    fun getTicketById(id: Long): Flow<GetTicketById?> {
        return queries.getTicketById(id).asFlow().mapToOneOrNull(Dispatchers.IO)
    }

    suspend fun insertMaintenanceTicket(subscriberId: Long?, issueType: String, description: String?, status: String) {
        val reportedDate = Clock.System.now().toEpochMilliseconds()
        queries.insertMaintenanceTicket(subscriberId, issueType, description, status, reportedDate)
    }

    suspend fun updateTicketStatus(id: Long, status: String) {
        queries.updateTicketStatus(status, id)
    }

    suspend fun deleteTicket(id: Long) {
        queries.deleteTicket(id)
    }

    // ===== Pricing Tier Operations =====
    fun getAllPricingTiers(): Flow<List<PricingTier>> {
        return queries.getAllPricingTiers().asFlow().mapToList(Dispatchers.IO)
    }

    suspend fun insertPricingTier(minUsage: Long, maxUsage: Long, pricePerUnit: Double) {
        queries.insertPricingTier(minUsage, maxUsage, pricePerUnit)
    }

    suspend fun updatePricingTier(id: Long, minUsage: Long, maxUsage: Long, pricePerUnit: Double) {
        queries.updatePricingTier(minUsage, maxUsage, pricePerUnit, id)
    }

    suspend fun deletePricingTier(id: Long) {
        queries.deletePricingTier(id)
    }

    // ===== Utility: Calculate Invoice Amount =====
    suspend fun calculateInvoiceAmount(consumption: Long): Double {
        val tiers = queries.getAllPricingTiers().executeAsList()
        var remaining = consumption
        var total = 0.0

        for (tier in tiers.sortedBy { it.minUsage }) {
            if (remaining <= 0) break
            val tierRange = tier.maxUsage - tier.minUsage + 1
            val usedInTier = minOf(remaining, tierRange)
            total += usedInTier * tier.pricePerUnit
            remaining -= usedInTier
        }

        return total
    }
}
