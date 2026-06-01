package org.associations.project.billing

import org.associations.project.database.Invoice
import org.associations.project.database.Subscriber
import org.associations.project.database.AppSettings
import org.associations.project.reports.MonthlyReportData

interface PrintService {
    suspend fun printInvoice(invoice: Invoice, subscriber: Subscriber, settings: AppSettings)

    /**
     * Prints multiple invoices as a single print job (multi-page document).
     * Useful for bulk printing meter readings/invoices for a whole month.
     */
    suspend fun printInvoices(
        items: List<Pair<Invoice, Subscriber>>,
        settings: AppSettings,
        jobName: String = "Invoices",
    )

    /**
     * Prints a simplified payment reminder/notification (not a full invoice).
     * Used for notifying customers to come and pay.
     */
    suspend fun printNotification(
        invoice: Invoice,
        subscriber: Subscriber,
        settings: AppSettings,
    )

    /**
     * Prints a beautiful, professional A4 monthly report.
     */
    suspend fun printMonthlyReport(
        report: MonthlyReportData,
        settings: AppSettings
    )

    /**
     * Exports the monthly report as a PDF to the given [outputStream].
     * Platform implementation renders the report (e.g. via HTML/WebView) and writes PDF bytes.
     */
    suspend fun exportMonthlyReport(
        report: MonthlyReportData,
        settings: AppSettings,
        outputStream: java.io.OutputStream
    )
}

