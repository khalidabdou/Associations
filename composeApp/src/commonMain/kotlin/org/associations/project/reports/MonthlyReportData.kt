package org.associations.project.reports

import org.associations.project.database.GetAllInvoicesByMonth
import org.associations.project.database.TransactionTable
import org.associations.project.utils.MonthYear

data class MonthlyReportData(
    val monthYear: MonthYear,
    val totalConsumption: Long,
    val totalInvoicesCount: Long,
    val totalInvoicesAmount: Double,
    val paidInvoicesCount: Long,
    val paidInvoicesAmount: Double,
    val unpaidInvoicesCount: Long,
    val unpaidInvoicesAmount: Double,
    val totalIncomeAmount: Double,
    val totalExpensesAmount: Double,
    val netBalance: Double,
    val invoices: List<GetAllInvoicesByMonth>,
    val transactions: List<TransactionTable>
)
