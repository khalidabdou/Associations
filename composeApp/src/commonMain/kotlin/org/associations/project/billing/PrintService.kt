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

    /**
     * Returns a list of paired Bluetooth thermal printers.
     * Returns empty list on platforms without Bluetooth support (Desktop).
     */
    suspend fun getPairedBluetoothPrinters(): List<BluetoothPrinterInfo>

    /**
     * Prints an invoice directly to a Bluetooth thermal printer using ESC/POS.
     * Only supported on Android with a POS print format.
     *
     * @param invoice The invoice to print
     * @param subscriber The subscriber info
     * @param settings App settings (print format, association info, etc.)
     * @param deviceAddress Bluetooth MAC address of the target printer
     * @return Result indicating success or failure with a message
     */
    suspend fun printInvoiceViaBluetooth(
        invoice: Invoice,
        subscriber: Subscriber,
        settings: AppSettings,
        deviceAddress: String
    ): Result<Unit>

    /**
     * Prints a payment notification directly to a Bluetooth thermal printer.
     *
     * @param invoice The invoice for the notification
     * @param subscriber The subscriber info
     * @param settings App settings
     * @param deviceAddress Bluetooth MAC address of the target printer
     * @return Result indicating success or failure with a message
     */
    suspend fun printNotificationViaBluetooth(
        invoice: Invoice,
        subscriber: Subscriber,
        settings: AppSettings,
        deviceAddress: String
    ): Result<Unit>

    /**
     * Prints a test page to a Bluetooth thermal printer.
     * Used to verify the printer connection and settings.
     *
     * @param context Any platform context (Android Context on Android)
     * @param deviceAddress Bluetooth MAC address of the target printer
     * @return Result indicating success or failure
     */
    suspend fun testBluetoothPrint(deviceAddress: String): Result<Unit>

    // ── USB thermal printer ──

    /**
     * Returns a list of connected USB thermal printers.
     * Returns empty list on platforms without USB Host support (Desktop).
     */
    suspend fun getConnectedUsbPrinters(): List<UsbPrinterInfo>

    /**
     * Prints an invoice directly to a USB thermal printer using ESC/POS.
     *
     * @param invoice The invoice to print
     * @param subscriber The subscriber info
     * @param settings App settings
     * @param deviceId USB device ID of the target printer
     * @return Result indicating success or failure
     */
    suspend fun printInvoiceViaUsb(
        invoice: Invoice,
        subscriber: Subscriber,
        settings: AppSettings,
        deviceId: Int
    ): Result<Unit>

    /**
     * Prints a payment notification directly to a USB thermal printer.
     *
     * @param invoice The invoice for the notification
     * @param subscriber The subscriber info
     * @param settings App settings
     * @param deviceId USB device ID of the target printer
     * @return Result indicating success or failure
     */
    suspend fun printNotificationViaUsb(
        invoice: Invoice,
        subscriber: Subscriber,
        settings: AppSettings,
        deviceId: Int
    ): Result<Unit>

    /**
     * Prints a test page to a USB thermal printer.
     *
     * @param deviceId USB device ID of the target printer
     * @return Result indicating success or failure
     */
    suspend fun testUsbPrint(deviceId: Int): Result<Unit>
}
