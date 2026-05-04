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

class AppRepository(db: AppDatabase, private val driverFactory: DatabaseDriverFactory) {
    private val queries = db.appDatabaseQueries

    // ===== Database Management =====
    fun exportDatabase(path: String) {
        try {
            driverFactory.exportDatabase(path)
        } catch (e: Exception) {
            println("Export error: ${e.message}")
            throw e
        }
    }

    fun importDatabase(path: String) {
        try {
            driverFactory.importDatabase(path)
        } catch (e: Exception) {
            println("Import error: ${e.message}")
            throw e
        }
    }

    fun exportDatabaseToStream(out: java.io.OutputStream) {
        try {
            driverFactory.exportDatabaseToStream(out)
        } catch (e: Exception) {
            println("Export error: ${e.message}")
            throw e
        }
    }

    fun importDatabaseFromStream(input: java.io.InputStream) {
        try {
            driverFactory.importDatabaseFromStream(input)
        } catch (e: Exception) {
            println("Import error: ${e.message}")
            throw e
        }
    }

    fun clearDatabase() {
        // Deprecated: use clearUserData instead
        try {
            driverFactory.clearDatabase()
        } catch (e: Exception) {
            println("Clear DB error: ${e.message}")
            throw e
        }
    }

    suspend fun clearUserData() {
        queries.transaction {
            queries.deleteAllInvoices()
            queries.deleteAllTransactions()
            queries.deleteAllTickets()
            queries.deleteAllSubscribers()
            queries.deleteAllZones()
            queries.deleteAllPricingTiers()
            // We DO NOT delete settings to preserve association info
        }
        // Re-initialize default data
        initializeDatabase()
    }

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
            // Ensure settings exist
            queries.initSettings()
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

    suspend fun insertSubscriber(
            fullName: String,
            phone: String?,
            meterNumber: String,
            address: String?,
            zoneId: Long
    ) {
        val createdAt = Clock.System.now().toEpochMilliseconds()
        queries.insertSubscriber(fullName, phone, meterNumber, address, zoneId, 1, createdAt)
    }

    suspend fun updateSubscriber(
            id: Long,
            fullName: String,
            phone: String?,
            meterNumber: String,
            address: String?,
            zoneId: Long,
            isActive: Long
    ) {
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

    suspend fun getLatestInvoiceBySubscriber(subscriberId: Long): Invoice? {
        return queries.getLatestInvoiceBySubscriber(subscriberId).executeAsOneOrNull()
    }

    suspend fun getLatestInvoiceBeforeDate(subscriberId: Long, date: Long): Invoice? {
        return queries.getLatestInvoiceBeforeDate(subscriberId, date).executeAsOneOrNull()
    }

    suspend fun checkExistingInvoice(subscriberId: Long, startDate: Long, endDate: Long): Invoice? {
        return queries.checkExistingInvoice(subscriberId, startDate, endDate).executeAsOneOrNull()
    }

    suspend fun updateInvoice(
            id: Long,
            currentReading: Long,
            consumption: Long,
            totalAmount: Double
    ) {
        queries.updateInvoice(currentReading, consumption, totalAmount, id)
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
            dueDate: Long,
            isPenaltyApplied: Long = 0
    ) {
        queries.insertInvoice(
                subscriberId,
                previousReading,
                currentReading,
                consumption,
                totalAmount,
                status,
                issueDate,
                dueDate,
                isPenaltyApplied
        )
    }

    suspend fun updateInvoiceStatus(id: Long, status: String) {
        queries.updateInvoiceStatus(status, id)
    }

    suspend fun getInvoiceDetailsOnce(id: Long): GetInvoiceById? {
        return queries.getInvoiceById(id).executeAsOneOrNull()
    }

    suspend fun deleteInvoice(id: Long) {
        queries.deleteInvoice(id)
    }

    fun getTotalUnpaidAmount(): Flow<Double> {
        return queries.getTotalUnpaidAmount().asFlow().mapToOneOrNull(Dispatchers.IO).map {
            it ?: 0.0
        }
    }

    fun getTotalConsumption(): Flow<Long> {
        return queries.getTotalConsumption().asFlow().mapToOneOrNull(Dispatchers.IO).map {
            it ?: 0L
        }
    }

    // ===== Transaction Operations =====
    fun getAllTransactions(): Flow<List<TransactionTable>> {
        return queries.getAllTransactions().asFlow().mapToList(Dispatchers.IO)
    }

    fun getTransactionsByType(type: String): Flow<List<TransactionTable>> {
        return queries.getTransactionsByType(type).asFlow().mapToList(Dispatchers.IO)
    }

    suspend fun insertTransaction(
            type: String,
            category: String,
            amount: Double,
            description: String?,
            date: Long
    ) {
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

    suspend fun insertMaintenanceTicket(
            subscriberId: Long?,
            issueType: String,
            description: String?,
            status: String
    ) {
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

    // ===== App Settings Operations =====
    fun getSettings(): Flow<AppSettings?> {
        return queries.getSettings().asFlow().mapToOneOrNull(Dispatchers.IO)
    }

    suspend fun updateSettings(
            lateFee: Double,
            monthlyFee: Double,
            gracePeriod: Int,
            dueDays: Int,
            associationName: String,
            associationAddress: String,
            associationPhone: String,
            printFormat: String,
            logoPath: String?
    ) {
        queries.updateSettings(
                lateFee,
                monthlyFee,
                gracePeriod.toLong(),
                dueDays.toLong(),
                associationName,
                associationAddress,
                associationPhone,
                printFormat,
                logoPath
        )
    }

    // ===== Utility: Calculate Invoice Amount =====
    suspend fun calculateInvoiceAmount(consumption: Long): Double {
        val tiers = queries.getAllPricingTiers().executeAsList().sortedBy { it.minUsage }
        val settings = queries.getSettings().executeAsOneOrNull()

        var total = 0.0

        // Find the applicable tier for the TOTAL consumption
        // Example: If consumption is 10, and tiers are 0-5(5dh), 6-15(8dh).
        // 10 falls in 6-15, so total = 10 * 8.

        if (tiers.isNotEmpty()) {
            val applicableTier =
                    tiers.find { consumption in it.minUsage..it.maxUsage }
                            ?: tiers.lastOrNull {
                                consumption > it.maxUsage
                            } // Use highest tier if exceeds range
                             ?: tiers.first() // Should not happen if tiers start at 0

            total = consumption * applicableTier.pricePerUnit
        }

        // Add Monthly Fixed Fee (if any)
        settings?.let { total += it.monthlyFixedFee }

        return total
    }

    suspend fun checkAndApplyLateFees() {
        val settings = queries.getSettings().executeAsOneOrNull() ?: return
        if (settings.lateFeeAmount <= 0.0) return

        val unpaidInvoices = queries.getUnpaidInvoices().executeAsList()
        val currentTime = Clock.System.now().toEpochMilliseconds()

        unpaidInvoices.forEach { invoice ->
            // Check if due date passed and penalty not applied
            // Note: getAllInvoices query result (GetUnpaidInvoices) now includes isPenaltyApplied?
            // Yes, "SELECT Invoice.* ..." includes checks.
            // BUT SQLDelight might generate a specific data class GetUnpaidInvoices that contains
            // the columns.
            // Since we added column to table, it should be there.

            if (invoice.isPenaltyApplied == 0L && currentTime > invoice.dueDate) {
                queries.applyPenalty(settings.lateFeeAmount, invoice.id)
            }
        }
    }

    // ===== Advanced Queries =====
    fun getTransactionsByMonth(month: Int, year: Int): Flow<List<TransactionTable>> {
        // Calculate start and end timestamps for the month
        // Note: Simple approximation, ideal solution would use kotlinx-datetime properly
        // For now we rely on the ViewModel to pass correct timestamps or handle it here
        // But since we store epoch millis, let's assume the VM passes start/end millis
        return queries.getAllTransactions()
                .asFlow()
                .mapToList(Dispatchers.IO) // Placeholder for now, will implement properly
    }

    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionTable>> {
        return queries.getAllTransactionsByMonth(startDate, endDate)
                .asFlow()
                .mapToList(Dispatchers.IO)
    }

    fun getInvoicesByDateRange(startDate: Long, endDate: Long): Flow<List<GetAllInvoicesByMonth>> {
        return queries.getAllInvoicesByMonth(startDate, endDate).asFlow().mapToList(Dispatchers.IO)
    }

    suspend fun updateTransaction(
            id: Long,
            type: String,
            category: String,
            amount: Double,
            description: String?,
            date: Long
    ) {
        queries.updateTransaction(type, category, amount, description, date, id)
    }

    // ===== Recent Activity Operations for Dashboard =====
    fun getRecentPaidInvoices(): Flow<List<GetRecentPaidInvoices>> {
        return queries.getRecentPaidInvoices().asFlow().mapToList(Dispatchers.IO)
    }

    fun getRecentSubscribers(): Flow<List<GetRecentSubscribers>> {
        return queries.getRecentSubscribers().asFlow().mapToList(Dispatchers.IO)
    }

    fun getRecentInvoices(): Flow<List<GetRecentInvoices>> {
        return queries.getRecentInvoices().asFlow().mapToList(Dispatchers.IO)
    }

    fun getRecentTransactions(): Flow<List<TransactionTable>> {
        return queries.getRecentTransactions().asFlow().mapToList(Dispatchers.IO)
    }
}
