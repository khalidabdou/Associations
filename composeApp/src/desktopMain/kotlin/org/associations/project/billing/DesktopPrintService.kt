package org.associations.project.billing

import org.associations.project.database.Invoice
import org.associations.project.database.Subscriber
import org.associations.project.database.AppSettings
import org.associations.project.reports.MonthlyReportData
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

        val isReceipt = settings.printFormat == "RECEIPT"
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
                g2d.font = Font("Dialog", Font.BOLD, titleSize)
                g2d.color = Color.BLACK
                centerString(g2d, settings.associationName, y, width)
                y += if (isReceipt) 20 else 25

                // Address and phone (centered)
                g2d.font = Font("Dialog", Font.PLAIN, bodySize)
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
                g2d.font = Font("Dialog", Font.BOLD, if (isReceipt) 12 else 16)
                val title = if (isPaid) "وصل دفع فاتورة الماء" else "فاتورة استهلاك الماء"
                centerString(g2d, title, y, width)
                y += if (isReceipt) 18 else 25

                // Paid stamp
                if (isPaid) {
                    g2d.color = Color(27, 94, 32)
                    g2d.font = Font("Dialog", Font.BOLD, bodySize)
                    centerString(g2d, "✓ مدفوعة", y, width)
                    g2d.color = Color.BLACK
                    y += if (isReceipt) 16 else 20
                }

                // Invoice info
                g2d.font = Font("Dialog", Font.PLAIN, bodySize)
                val dateLabel = if (isPaid) "تاريخ الدفع" else "التاريخ"
                g2d.drawString("رقم الفاتورة: ${invoice.id}", marginX, y)
                g2d.drawString("$dateLabel: ${formatDate(invoice.issueDate)}", marginX + contentWidth / 2, y)
                y += if (isReceipt) 14 else 18
                g2d.drawString("المشترك: ${subscriber.fullName}", marginX, y)
                g2d.drawString("العداد: ${subscriber.meterNumber}", marginX + contentWidth / 2, y)
                y += if (isReceipt) 20 else 30

                // Table header
                g2d.font = Font("Dialog", Font.BOLD, tableHeaderSize)
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
                g2d.font = Font("Dialog", Font.PLAIN, bodySize)
                g2d.drawString("${invoice.currentReading}", col1, y)
                g2d.drawString("${invoice.previousReading}", col2, y)
                g2d.drawString("${invoice.consumption} م³", col3, y)
                y += if (isReceipt) 15 else 25
                g2d.drawLine(marginX, y, width - marginX, y)
                y += if (isReceipt) 10 else 15

                // Breakdown
                g2d.font = Font("Dialog", Font.PLAIN, bodySize)
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
                g2d.font = Font("Dialog", Font.BOLD, totalSize)
                if (isPaid) g2d.color = Color(27, 94, 32)
                g2d.drawString("المجموع الكلي", labelX, y)
                rightAlignString(g2d, "${formatAmount(invoice.totalAmount)} درهم", y, valueX)
                g2d.color = Color.BLACK
                y += if (isReceipt) 25 else 35

                // Status box
                if (isPaid) {
                    g2d.color = Color(27, 94, 32)
                    g2d.font = Font("Dialog", Font.BOLD, bodySize)
                    centerString(g2d, "مدفوعة ✓ ${formatDate(invoice.issueDate)}", y, width)
                    g2d.color = Color.BLACK
                    y += if (isReceipt) 20 else 30
                } else if (invoice.dueDate > 0) {
                    g2d.color = Color(191, 54, 12)
                    g2d.font = Font("Dialog", Font.BOLD, bodySize)
                    centerString(g2d, "اجل الدفع: ${formatDate(invoice.dueDate)}", y, width)
                    g2d.color = Color.BLACK
                    y += if (isReceipt) 20 else 30
                }

                // Footer
                g2d.font = Font("Dialog", Font.ITALIC, bodySize)
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
                y += 50

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
        val job = PrinterJob.getPrinterJob()
        job.jobName = "التقرير الشهري - ${report.monthYear.displayName}"

        val pageFormat = job.defaultPage().clone() as PageFormat
        val paper = Paper()
        // A4: 210mm x 297mm
        val widthPt = 210.0 * 72.0 / 25.4
        val heightPt = 297.0 * 72.0 / 25.4
        paper.setSize(widthPt, heightPt)
        paper.setImageableArea(30.0, 30.0, widthPt - 60.0, heightPt - 60.0)
        pageFormat.paper = paper
        pageFormat.orientation = PageFormat.PORTRAIT

        val marginX = 40
        val bodySize = 12
        val titleSize = 18
        val lineGap = 16

        // Compute pages: summary page + invoice pages + transaction page
        val invoiceRowsPerPage = ((pageFormat.imageableHeight - lineGap * 5).toInt() / lineGap).coerceAtLeast(1)
        val invoicePages = if (report.invoices.isEmpty()) 0
            else (report.invoices.size + invoiceRowsPerPage - 1) / invoiceRowsPerPage
        val totalPages = 1 + invoicePages // page 0: summary, subsequent: invoices + transactions

        val printable = object : Printable {
            override fun print(graphics: Graphics, pf: PageFormat, pageIndex: Int): Int {
                if (pageIndex >= totalPages) return Printable.NO_SUCH_PAGE

                val g2d = graphics as Graphics2D
                g2d.translate(pf.imageableX, pf.imageableY)
                val width = pf.imageableWidth.toInt()
                val contentWidth = width - marginX * 2

                if (pageIndex == 0) {
                    // ── Summary page ──
                    var y = 30
                    g2d.font = getArabicFont(Font.BOLD, titleSize)
                    g2d.color = Color.BLACK
                    centerString(g2d, settings.associationName, y, width)
                    y += 20
                    g2d.font = getArabicFont(Font.PLAIN, bodySize)
                    if (settings.associationAddress.isNotBlank()) {
                        centerString(g2d, settings.associationAddress, y, width)
                        y += lineGap
                    }
                    if (settings.associationPhone.isNotBlank()) {
                        centerString(g2d, settings.associationPhone, y, width)
                        y += lineGap
                    }
                    y += 10
                    g2d.stroke = BasicStroke(1f)
                    g2d.drawLine(marginX, y, width - marginX, y)
                    y += 20

                    g2d.font = getArabicFont(Font.BOLD, 16)
                    centerString(g2d, "التقرير الشهري - ${report.monthYear.displayName}", y, width)
                    y += 30

                    // Summary box
                    g2d.color = Color(245, 245, 255)
                    g2d.fillRoundRect(marginX, y - 5, contentWidth, lineGap * 4 + 20, 10, 10)
                    g2d.color = Color(100, 100, 180)
                    g2d.drawRoundRect(marginX, y - 5, contentWidth, lineGap * 4 + 20, 10, 10)
                    g2d.color = Color.BLACK
                    g2d.font = getArabicFont(Font.PLAIN, bodySize)
                    y += lineGap
                    rightAlignString(g2d, "إجمالي الاستهلاك: ${report.totalConsumption} م³", y, width - marginX - 20)
                    y += lineGap
                    rightAlignString(g2d, "عدد الفواتير: ${report.totalInvoicesCount}  |  المبلغ: ${formatAmount(report.totalInvoicesAmount)} درهم", y, width - marginX - 20)
                    y += lineGap
                    rightAlignString(g2d, "المسددة: ${report.paidInvoicesCount}  |  المبلغ: ${formatAmount(report.paidInvoicesAmount)} درهم", y, width - marginX - 20)
                    y += lineGap
                    rightAlignString(g2d, "الغير مسددة: ${report.unpaidInvoicesCount}  |  المبلغ: ${formatAmount(report.unpaidInvoicesAmount)} درهم", y, width - marginX - 20)
                    y += 20

                    // Financial summary
                    g2d.color = Color(232, 245, 233)
                    g2d.fillRoundRect(marginX + 30, y - 5, contentWidth - 60, lineGap * 3 + 10, 8, 8)
                    g2d.color = Color(46, 125, 50)
                    g2d.drawRoundRect(marginX + 30, y - 5, contentWidth - 60, lineGap * 3 + 10, 8, 8)
                    g2d.color = Color.BLACK
                    g2d.font = getArabicFont(Font.BOLD, bodySize)
                    y += lineGap
                    rightAlignString(g2d, "الإيرادات: +${formatAmount(report.totalIncomeAmount)} درهم", y, width - marginX - 50)
                    y += lineGap
                    rightAlignString(g2d, "المصاريف: -${formatAmount(report.totalExpensesAmount)} درهم", y, width - marginX - 50)
                    y += lineGap
                    rightAlignString(g2d, "الرصيد الصافي: ${formatAmount(report.netBalance)} درهم", y, width - marginX - 50)
                    y += 30

                    // Page footer
                    g2d.font = getArabicFont(Font.PLAIN, 10)
                    centerString(g2d, "صفحة 1 / $totalPages", y + 150, width)

                    return Printable.PAGE_EXISTS
                }

                // ── Invoice/Transaction pages ──
                var y = 20
                g2d.font = getArabicFont(Font.BOLD, 14)
                g2d.color = Color.BLACK

                val invoiceStartIdx = (pageIndex - 1) * invoiceRowsPerPage
                val remainingInvoices = report.invoices.size - invoiceStartIdx

                if (remainingInvoices > 0) {
                    val rowsThisPage = minOf(remainingInvoices, invoiceRowsPerPage)
                    rightAlignString(g2d, "الفواتير${if (pageIndex > 1) " (تابع)" else ""}", y, width - marginX)
                    y += lineGap
                    g2d.drawLine(marginX, y, width - marginX, y)
                    y += 8

                    // Table header
                    g2d.font = getArabicFont(Font.BOLD, 10)
                    val colSub = marginX
                    val colMeter = marginX + contentWidth / 5
                    val colCons = marginX + contentWidth * 2 / 5
                    val colAmt = marginX + contentWidth * 3 / 5
                    val colStat = marginX + contentWidth * 4 / 5
                    g2d.drawString("المشترك", colSub, y)
                    g2d.drawString("العداد", colMeter, y)
                    g2d.drawString("الاستهلاك", colCons, y)
                    g2d.drawString("المبلغ", colAmt, y)
                    g2d.drawString("الحالة", colStat, y)
                    y += 6
                    g2d.drawLine(marginX, y, width - marginX, y)
                    y += lineGap

                    g2d.font = getArabicFont(Font.PLAIN, 10)
                    for (i in 0 until rowsThisPage) {
                        val inv = report.invoices[invoiceStartIdx + i]
                        g2d.drawString(inv.subscriberName ?: "", colSub, y)
                        g2d.drawString(inv.meterNumber ?: "", colMeter, y)
                        g2d.drawString("${inv.consumption} م³", colCons, y)
                        g2d.drawString(formatAmount(inv.totalAmount), colAmt, y)
                        val statusText = if (inv.status == "PAID") "مدفوعة" else "غير مدفوعة"
                        if (inv.status != "PAID") g2d.color = Color(191, 54, 12)
                        g2d.drawString(statusText, colStat, y)
                        g2d.color = Color.BLACK
                        y += lineGap
                    }
                }

                // If this is the last invoice page and we have transactions, add them
                val invoiceEndIdx = invoiceStartIdx + minOf(remainingInvoices, invoiceRowsPerPage)
                if (invoiceEndIdx >= report.invoices.size && report.transactions.isNotEmpty()) {
                    y += 20
                    g2d.font = getArabicFont(Font.BOLD, 14)
                    rightAlignString(g2d, "المعاملات المالية", y, width - marginX)
                    y += lineGap
                    g2d.drawLine(marginX, y, width - marginX, y)
                    y += 8

                    g2d.font = getArabicFont(Font.BOLD, 10)
                    val colTType = marginX
                    val colTCat = marginX + contentWidth / 7
                    val colTAmt = marginX + contentWidth * 3 / 7
                    val colTDesc = marginX + contentWidth * 5 / 7
                    g2d.drawString("النوع", colTType, y)
                    g2d.drawString("الصنف", colTCat, y)
                    g2d.drawString("المبلغ", colTAmt, y)
                    g2d.drawString("الوصف", colTDesc, y)
                    y += 6
                    g2d.drawLine(marginX, y, width - marginX, y)
                    y += lineGap

                    g2d.font = getArabicFont(Font.PLAIN, 10)
                    for (txn in report.transactions) {
                        if (y > pf.imageableHeight - 60) break
                        val typeText = if (txn.type == "INCOME") "إيراد" else "مصروف"
                        g2d.color = if (txn.type == "EXPENSE") Color(191, 54, 12) else Color(27, 94, 32)
                        g2d.drawString(typeText, colTType, y)
                        g2d.color = Color.BLACK
                        g2d.drawString(txn.category, colTCat, y)
                        g2d.drawString(formatAmount(txn.amount), colTAmt, y)
                        g2d.drawString(txn.description ?: "", colTDesc, y)
                        y += lineGap
                    }
                }

                // Footer
                g2d.font = getArabicFont(Font.PLAIN, 10)
                centerString(g2d, "صفحة ${pageIndex + 1} / $totalPages", pf.imageableHeight.toInt() - 40, width)

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
}
