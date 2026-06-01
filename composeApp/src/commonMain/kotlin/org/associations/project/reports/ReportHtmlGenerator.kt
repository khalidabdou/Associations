package org.associations.project.reports

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.associations.project.database.AppSettings
import org.associations.project.database.GetAllInvoicesByMonth

object ReportHtmlGenerator {

    fun generateMonthlyReportHtml(report: MonthlyReportData, settings: AppSettings): String {
        val paidInvoices = report.invoices.filter { it.status == "PAID" }
        val unpaidInvoices = report.invoices.filter { it.status != "PAID" }

        val monthYear = report.monthYear.displayName
        val startDate = formatDate(report.monthYear.startEpochMillis)
        val endDate = formatDate(report.monthYear.endEpochMillis)
        val now = kotlinx.datetime.Clock.System.now()
        val currentDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
        val currentDate = "${currentDateTime.dayOfMonth.toString().padStart(2, '0')}/${currentDateTime.monthNumber.toString().padStart(2, '0')}/${currentDateTime.year}"
        val currentTime = "${currentDateTime.hour.toString().padStart(2, '0')}:${currentDateTime.minute.toString().padStart(2, '0')}:${currentDateTime.second.toString().padStart(2, '0')}"

        return buildString {
            appendLine("<!DOCTYPE html>")
            appendLine("<html dir=\"rtl\" lang=\"ar\">")
            appendLine("<head>")
            appendLine("<meta charset=\"UTF-8\">")
            appendLine("<style>")
            appendLine("""
                @page { size: A4; margin: 15mm; }
                body { font-family: "Noto Sans Arabic", "Noto Naskh Arabic", Tahoma, Arial, sans-serif; margin: 0; padding: 10px; direction: rtl; color: #000; background: #fff; font-size: 11px; }
                .header { text-align: center; margin-bottom: 8px; }
                .header h1 { margin: 4px 0; font-size: 20px; font-weight: bold; }
                .header p { margin: 2px 0; font-size: 12px; color: #333; }
                .meta-row { width: 100%; border-collapse: collapse; font-size: 11px; margin-bottom: 10px; border-bottom: 1px solid #333; }
                .meta-row td { border: none; padding: 3px 8px; text-align: center; }
                .report-title { text-align: center; font-size: 18px; font-weight: bold; margin: 10px 0; padding: 6px 0; }
                .section-title { background: #ffc107; color: #000; padding: 6px 10px; font-weight: bold; font-size: 13px; margin-top: 15px; margin-bottom: 4px; text-align: center; border: 1px solid #333; }
                table { width: 100%; border-collapse: collapse; font-size: 10px; }
                th, td { border: 1px solid #333; padding: 4px 3px; text-align: center; vertical-align: middle; }
                th { background: #f5f5f5; font-weight: bold; }
                .total-row { background: #ffc107; font-weight: bold; }
                .grand-total { background: #2196F3; color: white; font-weight: bold; }
                .summary-table { width: 60%; margin: 10px auto; font-size: 12px; }
                .summary-table td { padding: 6px; }
                .footer { text-align: center; font-size: 10px; color: #555; margin-top: 15px; border-top: 1px solid #999; padding-top: 5px; }
                .page-break { page-break-before: always; }
                .nowrap { white-space: nowrap; }
            """.trimIndent())
            appendLine("</style>")
            appendLine("</head>")
            appendLine("<body>")

            // Header
            appendLine("<div class=\"header\">")
            appendLine("<h1>${settings.associationName.htmlEscape()}</h1>")
            if (settings.associationAddress.isNotBlank()) {
                appendLine("<p>${settings.associationAddress.htmlEscape()}</p>")
            }
            if (settings.associationPhone.isNotBlank()) {
                appendLine("<p>${settings.associationPhone.htmlEscape()}</p>")
            }
            appendLine("</div>")

            // Meta row
            appendLine("<table class=\"meta-row\">")
            appendLine("<tr>")
            appendLine("<td>المركز/الدوار: ${settings.associationAddress.htmlEscape()}</td>")
            appendLine("<td>إلى: ${endDate}</td>")
            appendLine("<td>من: ${startDate}</td>")
            appendLine("<td>سنة: ${report.monthYear.year}</td>")
            appendLine("</tr>")
            appendLine("</table>")

            // Report title
            appendLine("<div class=\"report-title\">تقرير ماء الشرب - ${monthYear.htmlEscape()}</div>")

            // Paid Invoices
            if (paidInvoices.isNotEmpty()) {
                appendLine("<div class=\"section-title\">الفواتير المستخلصة</div>")
                appendInvoiceTable(paidInvoices, settings, true)
            }

            // Unpaid Invoices
            if (unpaidInvoices.isNotEmpty()) {
                appendLine("<div class=\"section-title\">الفواتير غير المستخلصة</div>")
                appendInvoiceTable(unpaidInvoices, settings, false)
            }

            // Grand totals
            appendLine("<table>")
            appendLine("<tr class=\"total-row\">")
            appendLine("<td colspan=\"6\">مجموع الفواتير المستخلصة</td>")
            appendLine("<td>${formatAmount(report.paidInvoicesAmount)} DH</td>")
            appendLine("<td>${report.paidInvoicesCount}</td>")
            appendLine("</tr>")
            appendLine("<tr class=\"total-row\">")
            appendLine("<td colspan=\"6\">مجموع الفواتير غير المستخلصة</td>")
            appendLine("<td>${formatAmount(report.unpaidInvoicesAmount)} DH</td>")
            appendLine("<td>${report.unpaidInvoicesCount}</td>")
            appendLine("</tr>")
            appendLine("<tr class=\"grand-total\">")
            appendLine("<td colspan=\"6\">المجموع الكلي</td>")
            appendLine("<td>${formatAmount(report.totalInvoicesAmount)} DH</td>")
            appendLine("<td>${report.totalInvoicesCount}</td>")
            appendLine("</tr>")
            appendLine("</table>")

            // Financial summary
            appendLine("<div class=\"section-title\">الملخص المالي</div>")
            appendLine("<table class=\"summary-table\">")
            appendLine("<tr><td>إجمالي الاستهلاك</td><td>${report.totalConsumption} م³</td></tr>")
            appendLine("<tr><td>إجمالي الفواتير</td><td>${formatAmount(report.totalInvoicesAmount)} DH</td></tr>")
            appendLine("<tr><td>الفواتير المسددة</td><td>${formatAmount(report.paidInvoicesAmount)} DH</td></tr>")
            appendLine("<tr><td>الفواتير الغير مسددة</td><td>${formatAmount(report.unpaidInvoicesAmount)} DH</td></tr>")
            appendLine("<tr><td>الإيرادات</td><td>+${formatAmount(report.totalIncomeAmount)} DH</td></tr>")
            appendLine("<tr><td>المصاريف</td><td>-${formatAmount(report.totalExpensesAmount)} DH</td></tr>")
            appendLine("<tr class=\"grand-total\"><td>الرصيد الصافي</td><td>${formatAmount(report.netBalance)} DH</td></tr>")
            appendLine("</table>")

            // Transactions
            if (report.transactions.isNotEmpty()) {
                appendLine("<div class=\"section-title\">المعاملات المالية</div>")
                appendLine("<table>")
                appendLine("<thead>")
                appendLine("<tr>")
                appendLine("<th>التاريخ</th>")
                appendLine("<th>النوع</th>")
                appendLine("<th>الصنف</th>")
                appendLine("<th>المبلغ</th>")
                appendLine("<th>الوصف</th>")
                appendLine("</tr>")
                appendLine("</thead>")
                appendLine("<tbody>")
                for (txn in report.transactions) {
                    val typeText = if (txn.type == "INCOME") "إيراد" else "مصروف"
                    val typeColor = if (txn.type == "EXPENSE") "#b71c1c" else "#1b5e20"
                    val dateStr = formatDate(txn.date)
                    appendLine("<tr>")
                    appendLine("<td>${dateStr}</td>")
                    appendLine("<td style=\"color:${typeColor};font-weight:bold;\">${typeText}</td>")
                    appendLine("<td>${txn.category.htmlEscape()}</td>")
                    appendLine("<td>${formatAmount(txn.amount)} DH</td>")
                    appendLine("<td>${(txn.description ?: "").htmlEscape()}</td>")
                    appendLine("</tr>")
                }
                appendLine("</tbody>")
                appendLine("</table>")
            }

            // Footer
            appendLine("<div class=\"footer\">")
            appendLine("تم إنشاء هذا التقرير بتاريخ: ${currentDate} ${currentTime}")
            appendLine("</div>")

            appendLine("</body>")
            appendLine("</html>")
        }
    }

    private fun StringBuilder.appendInvoiceTable(
        invoices: List<GetAllInvoicesByMonth>,
        settings: AppSettings,
        isPaid: Boolean
    ) {
        val monthlyFee = settings.monthlyFixedFee
        val lateFee = settings.lateFeeAmount

        var totalWaterCharge = 0.0
        var totalPenalty = 0.0
        var totalCondition = 0.0
        var totalDue1 = 0.0
        var totalDue2 = 0.0
        var totalAmount = 0.0

        appendLine("<table>")
        appendLine("<thead>")
        appendLine("<tr>")
        appendLine("<th>المجموع</th>")
        appendLine("<th>الواجب 2</th>")
        appendLine("<th>الواجب 1</th>")
        appendLine("<th>و. الشرط</th>")
        appendLine("<th>غرامة التأخير</th>")
        appendLine("<th>و. الاستهلاك</th>")
        appendLine("<th>أجل التسديد</th>")
        appendLine("<th>تاريخ القراءة</th>")
        appendLine("<th>الفرق</th>")
        appendLine("<th>الجديد</th>")
        appendLine("<th>القديم</th>")
        appendLine("<th>اسم المشترك</th>")
        appendLine("<th>رقم العداد</th>")
        appendLine("</tr>")
        appendLine("</thead>")
        appendLine("<tbody>")

        for (inv in invoices) {
            val penalty = if (inv.isPenaltyApplied == 1L) lateFee else 0.0
            val waterCharge = (inv.totalAmount - penalty - monthlyFee).coerceAtLeast(0.0)
            val conditionFee = 0.0 // No data for this in current model
            val due1 = monthlyFee
            val due2 = 0.0

            totalWaterCharge += waterCharge
            totalPenalty += penalty
            totalCondition += conditionFee
            totalDue1 += due1
            totalDue2 += due2
            totalAmount += inv.totalAmount

            val issueDateStr = formatDate(inv.issueDate)
            val dueDateStr = if (inv.dueDate > 0) formatDate(inv.dueDate) else "-"

            appendLine("<tr>")
            appendLine("<td class=\"nowrap\">${formatAmount(inv.totalAmount)}</td>")
            appendLine("<td class=\"nowrap\">${formatAmount(due2)}</td>")
            appendLine("<td class=\"nowrap\">${formatAmount(due1)}</td>")
            appendLine("<td class=\"nowrap\">${formatAmount(conditionFee)}</td>")
            appendLine("<td class=\"nowrap\">${formatAmount(penalty)}</td>")
            appendLine("<td class=\"nowrap\">${formatAmount(waterCharge)}</td>")
            appendLine("<td class=\"nowrap\">${dueDateStr}</td>")
            appendLine("<td class=\"nowrap\">${issueDateStr}</td>")
            appendLine("<td class=\"nowrap\">${inv.consumption}</td>")
            appendLine("<td class=\"nowrap\">${inv.currentReading}</td>")
            appendLine("<td class=\"nowrap\">${inv.previousReading}</td>")
            appendLine("<td>${(inv.subscriberName ?: "").htmlEscape()}</td>")
            appendLine("<td class=\"nowrap\">${(inv.meterNumber ?: "").htmlEscape()}</td>")
            appendLine("</tr>")
        }

        appendLine("</tbody>")
        appendLine("<tfoot>")
        appendLine("<tr class=\"total-row\">")
        appendLine("<td class=\"nowrap\">${formatAmount(totalAmount)} DH</td>")
        appendLine("<td class=\"nowrap\">${formatAmount(totalDue2)} DH</td>")
        appendLine("<td class=\"nowrap\">${formatAmount(totalDue1)} DH</td>")
        appendLine("<td class=\"nowrap\">${formatAmount(totalCondition)} DH</td>")
        appendLine("<td class=\"nowrap\">${formatAmount(totalPenalty)} DH</td>")
        appendLine("<td class=\"nowrap\">${formatAmount(totalWaterCharge)} DH</td>")
        appendLine("<td>-</td>")
        appendLine("<td>-</td>")
        appendLine("<td>-</td>")
        appendLine("<td>-</td>")
        appendLine("<td>-</td>")
        appendLine("<td>مجموع ${if (isPaid) "الفواتير المستخلصة" else "الفواتير غير المستخلصة"}</td>")
        appendLine("<td>${invoices.size}</td>")
        appendLine("</tr>")
        appendLine("</tfoot>")
        appendLine("</table>")
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

    private fun String.htmlEscape(): String {
        return this
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }
}
