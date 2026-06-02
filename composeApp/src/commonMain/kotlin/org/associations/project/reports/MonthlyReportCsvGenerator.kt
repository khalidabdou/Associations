package org.associations.project.reports

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.associations.project.database.AppSettings

object MonthlyReportCsvGenerator {

    fun generate(report: MonthlyReportData, settings: AppSettings): String {
        return buildString {
            // UTF-8 BOM for Excel compatibility
            append("\uFEFF")

            // Title
            appendLine("تقرير ماء الشرب - ${report.monthYear.displayName}")
            appendLine("${settings.associationName}, ${settings.associationAddress}, ${settings.associationPhone}")
            appendLine()

            // Summary
            appendLine("الملخص")
            appendLine("إجمالي الاستهلاك,${report.totalConsumption} م³")
            appendLine("عدد الفواتير,${report.totalInvoicesCount}")
            appendLine("مبلغ الفواتير,${formatAmount(report.totalInvoicesAmount)} DH")
            appendLine("فواتير مسددة,${report.paidInvoicesCount} | ${formatAmount(report.paidInvoicesAmount)} DH")
            appendLine("فواتير غير مسددة,${report.unpaidInvoicesCount} | ${formatAmount(report.unpaidInvoicesAmount)} DH")
            appendLine("إيرادات,${formatAmount(report.totalIncomeAmount)} DH")
            appendLine("مصاريف,${formatAmount(report.totalExpensesAmount)} DH")
            appendLine("الرصيد الصافي,${formatAmount(report.netBalance)} DH")
            appendLine()

            // Invoices
            appendLine("الفواتير")
            appendLine(
                "رقم الفاتورة,اسم المشترك,رقم العداد,القراءة القديمة,القراءة الجديدة,الاستهلاك," +
                "مبلغ الاستهلاك,الرسوم الشهرية,غرامة التأخير,المجموع,الحالة,تاريخ الإصدار,أجل التسديد"
            )
            val monthlyFee = settings.monthlyFixedFee
            val lateFee = settings.lateFeeAmount
            for (inv in report.invoices) {
                val penalty = if (inv.isPenaltyApplied == 1L) lateFee else 0.0
                val waterCharge = (inv.totalAmount - penalty - monthlyFee).coerceAtLeast(0.0)
                appendLine(
                    "${inv.id}," +
                    "${csvEscape(inv.subscriberName ?: "")}," +
                    "${csvEscape(inv.meterNumber ?: "")}," +
                    "${inv.previousReading}," +
                    "${inv.currentReading}," +
                    "${inv.consumption} م³," +
                    "${formatAmount(waterCharge)} DH," +
                    "${formatAmount(monthlyFee)} DH," +
                    "${formatAmount(penalty)} DH," +
                    "${formatAmount(inv.totalAmount)} DH," +
                    "${if (inv.status == "PAID") "مسددة" else "غير مسددة"}," +
                    "${formatDate(inv.issueDate)}," +
                    "${formatDate(inv.dueDate)}"
                )
            }
            appendLine()

            // Transactions
            if (report.transactions.isNotEmpty()) {
                appendLine("المعاملات المالية")
                appendLine("التاريخ,النوع,الصنف,المبلغ,الوصف")
                for (txn in report.transactions) {
                    appendLine(
                        "${formatDate(txn.date)}," +
                        "${if (txn.type == "INCOME") "إيراد" else "مصروف"}," +
                        "${csvEscape(txn.category)}," +
                        "${formatAmount(txn.amount)} DH," +
                        "${csvEscape(txn.description ?: "")}"
                    )
                }
            }
        }
    }

    private fun formatAmount(value: Double): String {
        val rounded = (value * 100).toLong() / 100.0
        return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
    }

    private fun formatDate(epochMillis: Long): String {
        if (epochMillis <= 0) return "-"
        val dateTime = Instant.fromEpochMilliseconds(epochMillis)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        val day = dateTime.dayOfMonth.toString().padStart(2, '0')
        val month = dateTime.monthNumber.toString().padStart(2, '0')
        val year = dateTime.year
        return "$day/$month/$year"
    }

    private fun csvEscape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            "\"$escaped\""
        } else {
            escaped
        }
    }
}
