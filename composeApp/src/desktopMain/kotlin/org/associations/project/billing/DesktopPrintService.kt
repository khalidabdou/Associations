package org.associations.project.billing

import org.associations.project.database.Invoice
import org.associations.project.database.Subscriber
import org.associations.project.database.AppSettings
import org.associations.project.reports.MonthlyReportData
import java.awt.Desktop
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.print.PageFormat
import java.awt.print.Paper
import java.awt.print.Printable
import java.awt.print.PrinterJob
import java.awt.Color
import java.awt.BasicStroke
import java.awt.GraphicsEnvironment
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class DesktopPrintService : PrintService {

    private fun formatAmount(amount: Double): String {
        return if (amount == amount.toLong().toDouble()) {
            "${amount.toLong()}"
        } else {
            String.format("%.2f", amount)
        }
    }

    private fun formatDate(epochMillis: Long): String {
        val instant = Instant.ofEpochMilli(epochMillis)
        val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        return String.format("%02d/%02d/%d", localDate.dayOfMonth, localDate.monthValue, localDate.year)
    }

    private fun centerString(g2d: Graphics2D, text: String, y: Int, width: Int) {
        val textWidth = g2d.fontMetrics.stringWidth(text)
        g2d.drawString(text, (width - textWidth) / 2, y)
    }

    private fun rightAlignString(g2d: Graphics2D, text: String, y: Int, rightX: Int) {
        val textWidth = g2d.fontMetrics.stringWidth(text)
        g2d.drawString(text, rightX - textWidth, y)
    }

    private fun getArabicFont(style: Int, size: Int): Font {
        val candidates = listOf("Arial", "Tahoma", "Segoe UI", "Noto Sans Arabic", "Droid Sans Arabic")
        for (family in candidates) {
            try {
                val font = Font(family, style, size)
                if (font.canDisplay('ا')) return font
            } catch (_: Exception) {
                // ignore and try next candidate
            }
        }
        return Font("Dialog", style, size)
    }

    override suspend fun printInvoice(invoice: Invoice, subscriber: Subscriber, settings: AppSettings) {
        printInvoices(
            items = listOf(invoice to subscriber),
            settings = settings,
            jobName = "فاتورة ${invoice.id} - ${subscriber.fullName}",
        )
    }

    override suspend fun printInvoices(
        items: List<Pair<Invoice, Subscriber>>,
        settings: AppSettings,
        jobName: String,
    ) {
        if (items.isEmpty()) return
        val job = PrinterJob.getPrinterJob()
        job.jobName = jobName

        val isReceipt = settings.printFormat == "RECEIPT" || settings.printFormat == "POS"
        val isA5 = settings.printFormat == "A5"

        val pageFormat = job.defaultPage().clone() as PageFormat
        val paper = Paper()

        when {
            isReceipt -> {
                // 80mm width x ~420mm height roll
                val widthPt = 80.0 * 72.0 / 25.4
                val heightPt = 420.0 * 72.0 / 25.4
                paper.setSize(widthPt, heightPt)
                paper.setImageableArea(8.0, 8.0, widthPt - 16.0, heightPt - 16.0)
            }
            isA5 -> {
                // A5: 148mm x 210mm
                val widthPt = 148.0 * 72.0 / 25.4
                val heightPt = 210.0 * 72.0 / 25.4
                paper.setSize(widthPt, heightPt)
                paper.setImageableArea(20.0, 20.0, widthPt - 40.0, heightPt - 40.0)
            }
            else -> {
                // A4: 210mm x 297mm
                val widthPt = 210.0 * 72.0 / 25.4
                val heightPt = 297.0 * 72.0 / 25.4
                paper.setSize(widthPt, heightPt)
                paper.setImageableArea(30.0, 30.0, widthPt - 60.0, heightPt - 60.0)
            }
        }

        pageFormat.paper = paper
        pageFormat.orientation = PageFormat.PORTRAIT

        val printable = object : Printable {
            override fun print(graphics: Graphics, pageFormat: PageFormat, pageIndex: Int): Int {
                if (pageIndex >= items.size) return Printable.NO_SUCH_PAGE
                val (invoice, subscriber) = items[pageIndex]

                val g2d = graphics as Graphics2D
                g2d.translate(pageFormat.imageableX, pageFormat.imageableY)

                val width = pageFormat.imageableWidth.toInt()
                val marginX = if (isReceipt) 10 else 40
                val contentWidth = width - marginX * 2
                var y = if (isReceipt) 20 else 40

                val isPaid = invoice.status == "PAID"
                val penaltyApplied = invoice.isPenaltyApplied == 1L
                val penaltyValue = if (penaltyApplied) settings.lateFeeAmount else 0.0
                val monthlyFeeValue = if (settings.monthlyFixedFee > 0.0) settings.monthlyFixedFee else 0.0
                val waterChargeValue = (invoice.totalAmount - penaltyValue - monthlyFeeValue).coerceAtLeast(0.0)

                // Title sizes based on format
                val titleSize = if (isReceipt) 14 else 18
                val bodySize = if (isReceipt) 10 else 12
                val tableHeaderSize = if (isReceipt) 10 else 12
                val totalSize = if (isReceipt) 14 else 16

                // Header: Association name (centered)
                g2d.font = getArabicFont(Font.BOLD, titleSize)
                g2d.color = Color.BLACK
                centerString(g2d, settings.associationName, y, width)
                y += if (isReceipt) 20 else 25

                // Address and phone (centered)
                g2d.font = getArabicFont(Font.PLAIN, bodySize)
                if (settings.associationAddress.isNotBlank()) {
                    centerString(g2d, settings.associationAddress, y, width)
                    y += if (isReceipt) 12 else 15
                }
                if (settings.associationPhone.isNotBlank()) {
                    centerString(g2d, settings.associationPhone, y, width)
                    y += if (isReceipt) 12 else 15
                }
                y += if (isReceipt) 10 else 20

                // Divider
                g2d.stroke = BasicStroke(1f)
                g2d.drawLine(marginX, y, width - marginX, y)
                y += if (isReceipt) 15 else 25

                // Title
                g2d.font = getArabicFont(Font.BOLD, if (isReceipt) 12 else 16)
                val title = if (isPaid) "وصل دفع فاتورة الماء" else "فاتورة استهلاك الماء"
                centerString(g2d, title, y, width)
                y += if (isReceipt) 18 else 25

                // Paid stamp
                if (isPaid) {
                    g2d.color = Color(27, 94, 32)
                    g2d.font = getArabicFont(Font.BOLD, bodySize)
                    centerString(g2d, "✓ مدفوعة", y, width)
                    g2d.color = Color.BLACK
                    y += if (isReceipt) 16 else 20
                }

                // Invoice info
                g2d.font = getArabicFont(Font.PLAIN, bodySize)
                val dateLabel = if (isPaid) "تاريخ الدفع" else "التاريخ"
                g2d.drawString("رقم الفاتورة: ${invoice.id}", marginX, y)
                g2d.drawString("$dateLabel: ${formatDate(invoice.issueDate)}", marginX + contentWidth / 2, y)
                y += if (isReceipt) 14 else 18
                g2d.drawString("المشترك: ${subscriber.fullName}", marginX, y)
                g2d.drawString("العداد: ${subscriber.meterNumber}", marginX + contentWidth / 2, y)
                y += if (isReceipt) 20 else 30

                // Table header
                g2d.font = getArabicFont(Font.BOLD, tableHeaderSize)
                val col1 = marginX
                val col2 = marginX + contentWidth / 3
                val col3 = marginX + contentWidth * 2 / 3
                g2d.drawString("الحالية", col1, y)
                g2d.drawString("السابقة", col2, y)
                g2d.drawString("الاستهلاك", col3, y)
                y += 5
                g2d.drawLine(marginX, y, width - marginX, y)
                y += if (isReceipt) 12 else 18

                // Table row
                g2d.font = getArabicFont(Font.PLAIN, bodySize)
                g2d.drawString("${invoice.currentReading}", col1, y)
                g2d.drawString("${invoice.previousReading}", col2, y)
                g2d.drawString("${invoice.consumption} م³", col3, y)
                y += if (isReceipt) 15 else 25
                g2d.drawLine(marginX, y, width - marginX, y)
                y += if (isReceipt) 10 else 15

                // Breakdown
                g2d.font = getArabicFont(Font.PLAIN, bodySize)
                val labelX = marginX
                val valueX = width - marginX
                g2d.drawString("استهلاك الماء", labelX, y)
                rightAlignString(g2d, "${formatAmount(waterChargeValue)} درهم", y, valueX)
                y += if (isReceipt) 14 else 18

                if (monthlyFeeValue > 0.0) {
                    g2d.drawString("الرسوم الشهرية", labelX, y)
                    rightAlignString(g2d, "${formatAmount(monthlyFeeValue)} درهم", y, valueX)
                    y += if (isReceipt) 14 else 18
                }

                if (penaltyValue > 0.0) {
                    g2d.color = Color(191, 54, 12)
                    g2d.drawString("غرامة التأخير", labelX, y)
                    rightAlignString(g2d, "${formatAmount(penaltyValue)} درهم", y, valueX)
                    g2d.color = Color.BLACK
                    y += if (isReceipt) 14 else 18
                }
                y += if (isReceipt) 8 else 12

                // Total
                g2d.font = getArabicFont(Font.BOLD, totalSize)
                if (isPaid) g2d.color = Color(27, 94, 32)
                g2d.drawString("المجموع الكلي", labelX, y)
                rightAlignString(g2d, "${formatAmount(invoice.totalAmount)} درهم", y, valueX)
                g2d.color = Color.BLACK
                y += if (isReceipt) 25 else 35

                // Status box
                if (isPaid) {
                    g2d.color = Color(27, 94, 32)
                    g2d.font = getArabicFont(Font.BOLD, bodySize)
                    centerString(g2d, "مدفوعة ✓ ${formatDate(invoice.issueDate)}", y, width)
                    g2d.color = Color.BLACK
                    y += if (isReceipt) 20 else 30
                } else if (invoice.dueDate > 0) {
                    g2d.color = Color(191, 54, 12)
                    g2d.font = getArabicFont(Font.BOLD, bodySize)
                    centerString(g2d, "اجل الدفع: ${formatDate(invoice.dueDate)}", y, width)
                    g2d.color = Color.BLACK
                    y += if (isReceipt) 20 else 30
                }

                // Footer
                g2d.font = getArabicFont(Font.ITALIC, bodySize)
                centerString(g2d, "شكرا لالتزامكم بتسديد واجباتكم", y, width)

                return Printable.PAGE_EXISTS
            }
        }

        job.setPrintable(printable, pageFormat)
        if (job.printDialog()) {
            try {
                job.print()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun printNotification(
        invoice: Invoice,
        subscriber: Subscriber,
        settings: AppSettings
    ) {
        val job = PrinterJob.getPrinterJob()
        job.jobName = "إشعار دفع - ${subscriber.fullName}"

        val pageFormat = job.defaultPage().clone() as PageFormat
        val paper = Paper()
        val widthPt = 148.0 * 72.0 / 25.4  // A5-ish for notifications
        val heightPt = 210.0 * 72.0 / 25.4
        paper.setSize(widthPt, heightPt)
        paper.setImageableArea(20.0, 20.0, widthPt - 40.0, heightPt - 40.0)
        pageFormat.paper = paper
        pageFormat.orientation = PageFormat.PORTRAIT

        val printable = object : Printable {
            override fun print(graphics: Graphics, pageFormat: PageFormat, pageIndex: Int): Int {
                if (pageIndex > 0) return Printable.NO_SUCH_PAGE

                val g2d = graphics as Graphics2D
                g2d.translate(pageFormat.imageableX, pageFormat.imageableY)
                val width = pageFormat.imageableWidth.toInt()
                val marginX = 40
                val contentWidth = width - marginX * 2
                var y = 40

                // Logo
                if (!settings.logoPath.isNullOrBlank()) {
                    try {
                        val logoFile = File(settings.logoPath)
                        if (logoFile.exists()) {
                            val logo: BufferedImage? = ImageIO.read(logoFile)
                            if (logo != null) {
                                val logoSize = 60
                                val logoLeft = (width - logoSize) / 2
                                g2d.drawImage(logo, logoLeft, y, logoSize, logoSize, null)
                                y += logoSize + 10
                            }
                        }
                    } catch (e: Exception) {
                        println("Error loading logo for print: ${e.message}")
                    }
                }

                // Header
                g2d.font = getArabicFont(Font.BOLD, 20)
                g2d.color = Color.BLACK
                centerString(g2d, settings.associationName, y, width)
                y += 25
                g2d.font = getArabicFont(Font.PLAIN, 12)
                if (settings.associationAddress.isNotBlank()) {
                    centerString(g2d, settings.associationAddress, y, width)
                    y += 15
                }
                if (settings.associationPhone.isNotBlank()) {
                    centerString(g2d, settings.associationPhone, y, width)
                    y += 15
                }
                y += 20

                // Divider
                g2d.stroke = BasicStroke(1f)
                g2d.drawLine(marginX, y, width - marginX, y)
                y += 25

                // Title
                g2d.font = getArabicFont(Font.BOLD, 18)
                centerString(g2d, "إشعار دفع", y, width)
                y += 35

                // Notification box background
                g2d.color = Color(255, 253, 231)
                g2d.fillRoundRect(marginX, y - 10, contentWidth, 160, 12, 12)
                g2d.color = Color(245, 176, 65)
                g2d.drawRoundRect(marginX, y - 10, contentWidth, 160, 12, 12)
                g2d.color = Color.BLACK

                // Subscriber info
                g2d.font = getArabicFont(Font.PLAIN, 12)
                val labelX = marginX + 20
                val valueX = width - marginX - 20
                g2d.drawString("المشترك:", labelX, y)
                rightAlignString(g2d, subscriber.fullName, y, valueX)
                y += 22
                g2d.drawString("العداد:", labelX, y)
                rightAlignString(g2d, subscriber.meterNumber, y, valueX)
                y += 22
                g2d.drawString("رقم الفاتورة:", labelX, y)
                rightAlignString(g2d, "${invoice.id}", y, valueX)
                y += 28

                // Amount due
                g2d.color = Color(191, 54, 12)
                g2d.font = getArabicFont(Font.BOLD, 14)
                g2d.drawString("المبلغ المستحق:", labelX, y)
                rightAlignString(g2d, "${formatAmount(invoice.totalAmount)} درهم", y, valueX)
                g2d.color = Color.BLACK
                y += 25

                // Penalty breakdown (if applied)
                val penaltyApplied = invoice.isPenaltyApplied == 1L
                val penaltyValue = if (penaltyApplied) settings.lateFeeAmount else 0.0
                if (penaltyValue > 0.0) {
                    g2d.font = getArabicFont(Font.PLAIN, 11)
                    g2d.color = Color(191, 54, 12)
                    g2d.drawString("بما فيها غرامة التأخير:", labelX, y)
                    rightAlignString(g2d, "${formatAmount(penaltyValue)} درهم", y, valueX)
                    g2d.color = Color.BLACK
                    y += 22
                }
                y += 25

                // Payment deadline
                if (invoice.dueDate > 0) {
                    g2d.color = Color(255, 243, 224)
                    g2d.fillRoundRect(marginX, y - 5, contentWidth, 40, 8, 8)
                    g2d.color = Color(230, 81, 0)
                    g2d.drawRoundRect(marginX, y - 5, contentWidth, 40, 8, 8)
                    g2d.color = Color(191, 54, 12)
                    g2d.font = getArabicFont(Font.BOLD, 12)
                    centerString(g2d, "اجل الدفع: ${formatDate(invoice.dueDate)}", y + 22, width)
                    g2d.color = Color.BLACK
                    y += 55
                }

                return Printable.PAGE_EXISTS
            }
        }

        job.setPrintable(printable, pageFormat)
        if (job.printDialog()) {
            try {
                job.print()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun printMonthlyReport(
        report: MonthlyReportData,
        settings: AppSettings
    ) {
        val pdfBytes = DesktopMonthlyReportRenderer.renderToPdfBytes(report, settings)
        println("[DesktopPrintService] Print: PDF bytes=${pdfBytes.size}")
        if (pdfBytes.isEmpty()) {
            println("[DesktopPrintService] Print: WARNING - PDF rendering produced 0 bytes!")
            return
        }
        val tempFile = File.createTempFile(
            "report_${report.monthYear.year}_${report.monthYear.month}_",
            ".pdf"
        )
        tempFile.writeBytes(pdfBytes)
        tempFile.deleteOnExit()
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(tempFile)
        }
    }

    override suspend fun exportMonthlyReport(
        report: MonthlyReportData,
        settings: AppSettings,
        outputStream: java.io.OutputStream
    ) {
        DesktopMonthlyReportRenderer.renderToPdf(report, settings, outputStream)
        outputStream.flush()
    }

    // ── Bluetooth — not supported on Desktop ──

    override suspend fun getPairedBluetoothPrinters(): List<BluetoothPrinterInfo> = emptyList()

    override suspend fun printInvoiceViaBluetooth(
        invoice: Invoice,
        subscriber: Subscriber,
        settings: AppSettings,
        deviceAddress: String
    ): Result<Unit> = Result.failure(UnsupportedOperationException("الطباعة عبر البلوتوث غير مدعومة على سطح المكتب"))

    override suspend fun printNotificationViaBluetooth(
        invoice: Invoice,
        subscriber: Subscriber,
        settings: AppSettings,
        deviceAddress: String
    ): Result<Unit> = Result.failure(UnsupportedOperationException("الطباعة عبر البلوتوث غير مدعومة على سطح المكتب"))

    override suspend fun testBluetoothPrint(deviceAddress: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("الطباعة عبر البلوتوث غير مدعومة على سطح المكتب"))
}
