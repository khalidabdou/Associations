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
import javax.print.PrintServiceLookup
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.delay
import org.associations.project.utils.ArabicShaper
import java.awt.font.TextLayout
import java.awt.font.TextAttribute
import java.text.AttributedString

class DesktopPrintService : PrintService {

    private val printMutex = Mutex()

    /**
     * Shape Arabic text for proper connected glyph forms.
     * Returns the shaped string ready for drawString (still LTR in buffer).
     */
    private fun ar(text: String): String {
        return ArabicShaper.shape(text)
    }

    /**
     * Draw Arabic/RTL text correctly using Java2D's Bidi + TextLayout pipeline.
     * This ensures characters are connected and displayed right-to-left.
     */
    private fun drawArabic(g2d: Graphics2D, text: String, x: Int, y: Int) {
        val shaped = ArabicShaper.shape(text)
        val atStr = AttributedString(shaped)
        atStr.addAttribute(TextAttribute.FONT, g2d.font)
        atStr.addAttribute(TextAttribute.RUN_DIRECTION, TextAttribute.RUN_DIRECTION_RTL)
        val layout = TextLayout(atStr.iterator, g2d.fontRenderContext)
        layout.draw(g2d, x.toFloat(), y.toFloat())
    }

    /**
     * Draw Arabic text centered horizontally.
     */
    private fun drawArabicCentered(g2d: Graphics2D, text: String, y: Int, width: Int) {
        val shaped = ArabicShaper.shape(text)
        val atStr = AttributedString(shaped)
        atStr.addAttribute(TextAttribute.FONT, g2d.font)
        atStr.addAttribute(TextAttribute.RUN_DIRECTION, TextAttribute.RUN_DIRECTION_RTL)
        val layout = TextLayout(atStr.iterator, g2d.fontRenderContext)
        val textWidth = layout.advance
        layout.draw(g2d, ((width - textWidth) / 2).toFloat(), y.toFloat())
    }

    /**
     * Draw Arabic text right-aligned (ending at rightX).
     */
    private fun drawArabicRight(g2d: Graphics2D, text: String, y: Int, rightX: Int) {
        val shaped = ArabicShaper.shape(text)
        val atStr = AttributedString(shaped)
        atStr.addAttribute(TextAttribute.FONT, g2d.font)
        atStr.addAttribute(TextAttribute.RUN_DIRECTION, TextAttribute.RUN_DIRECTION_RTL)
        val layout = TextLayout(atStr.iterator, g2d.fontRenderContext)
        val textWidth = layout.advance
        layout.draw(g2d, (rightX - textWidth), y.toFloat())
    }


    private class ImageWrapperPrintable(private val delegate: Printable) : Printable {
        override fun print(graphics: java.awt.Graphics, pageFormat: PageFormat, pageIndex: Int): Int {
            val g2d = graphics as Graphics2D
            
            // Render to BufferedImage first to bypass macOS AWT print shaping bugs
            val scale = 4.0 // 288 DPI
            val w = (pageFormat.width * scale).toInt()
            val h = (pageFormat.height * scale).toInt()
            
            val image = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
            val g = image.createGraphics()
            
            g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)
            g.setRenderingHint(java.awt.RenderingHints.KEY_FRACTIONALMETRICS, java.awt.RenderingHints.VALUE_FRACTIONALMETRICS_ON)
            
            g.color = Color.WHITE
            g.fillRect(0, 0, w, h)
            
            g.scale(scale, scale)
            
            val result = delegate.print(g, pageFormat, pageIndex)
            g.dispose()
            
            if (result == Printable.PAGE_EXISTS) {
                g2d.drawImage(image, 0, 0, pageFormat.width.toInt(), pageFormat.height.toInt(), null)
            }
            
            return result
        }
    }

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
        drawArabicCentered(g2d, text, y, width)
    }

    private fun rightAlignString(g2d: Graphics2D, text: String, y: Int, rightX: Int) {
        drawArabicRight(g2d, text, y, rightX)
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

    private fun createInvoicePrintable(
        items: List<Pair<Invoice, Subscriber>>,
        settings: AppSettings
    ): Printable {
        return Printable { graphics, pageFormat, pageIndex ->
            if (pageIndex >= items.size) return@Printable Printable.NO_SUCH_PAGE
            val (invoice, subscriber) = items[pageIndex]

            val g2d = graphics as Graphics2D
            g2d.translate(pageFormat.imageableX, pageFormat.imageableY)

            val isReceipt = settings.printFormat == "RECEIPT" || settings.printFormat == "POS"
            val isA5 = settings.printFormat == "A5"
            val textColor = if (isReceipt) Color.BLACK else Color(50, 50, 50)

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

            // Logo (A4/A5 only — receipt uses plain header for speed)
            if (!isReceipt && !settings.logoPath.isNullOrBlank()) {
                try {
                    val logoFile = File(settings.logoPath)
                    if (logoFile.exists()) {
                        val logo: BufferedImage? = ImageIO.read(logoFile)
                        if (logo != null) {
                            val logoSize = if (isA5) 50 else 65
                            val logoLeft = (width - logoSize) / 2
                            g2d.drawImage(logo, logoLeft, y, logoSize, logoSize, null)
                            y += logoSize + 12
                        }
                    }
                } catch (e: Exception) {
                    println("Error loading logo for invoice print: ${e.message}")
                }
            }

            // Header: Association name (centered)
            g2d.font = getArabicFont(Font.BOLD, titleSize)
            g2d.color = textColor
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
                g2d.color = textColor
                y += if (isReceipt) 16 else 20
            }

            // Invoice info (stacked on receipts to prevent text overlaps)
            g2d.font = getArabicFont(Font.PLAIN, bodySize)
            val dateLabel = if (isPaid) "تاريخ الدفع" else "التاريخ"
            if (isReceipt) {
                drawArabic(g2d, "المشترك: ${subscriber.fullName}", marginX, y)
                y += 14
                drawArabic(g2d, "العداد: ${subscriber.meterNumber}", marginX, y)
                y += 14
                drawArabic(g2d, "رقم الفاتورة: ${invoice.id}", marginX, y)
                y += 14
                drawArabic(g2d, "$dateLabel: ${formatDate(invoice.issueDate)}", marginX, y)
                y += 20
            } else {
                // A4/A5: left-aligned Arabic labels on left, right-aligned on right
                drawArabicRight(g2d, "المشترك: ${subscriber.fullName}", y, width - marginX)
                drawArabicRight(g2d, "رقم الفاتورة: ${invoice.id}", y, marginX + contentWidth / 2)
                y += 20
                drawArabicRight(g2d, "العداد: ${subscriber.meterNumber}", y, width - marginX)
                drawArabicRight(g2d, "$dateLabel: ${formatDate(invoice.issueDate)}", y, marginX + contentWidth / 2)
                y += 30
            }

            // Table header
            g2d.font = getArabicFont(Font.BOLD, tableHeaderSize)
            val col1 = marginX
            val col2 = marginX + contentWidth / 3
            val col3 = marginX + contentWidth * 2 / 3
            drawArabic(g2d, "الحالية", col1, y)
            drawArabic(g2d, "السابقة", col2, y)
            drawArabic(g2d, "الاستهلاك", col3, y)
            y += 5
            g2d.drawLine(marginX, y, width - marginX, y)
            y += if (isReceipt) 12 else 18

            // Table row
            g2d.font = getArabicFont(Font.PLAIN, bodySize)
            drawArabic(g2d, "${invoice.currentReading}", col1, y)
            drawArabic(g2d, "${invoice.previousReading}", col2, y)
            drawArabic(g2d, "${invoice.consumption} م³", col3, y)
            y += if (isReceipt) 15 else 25
            g2d.drawLine(marginX, y, width - marginX, y)
            y += if (isReceipt) 10 else 15

            // Breakdown
            g2d.font = getArabicFont(Font.PLAIN, bodySize)
            val labelX = marginX
            val valueX = width - marginX
            drawArabic(g2d, "استهلاك الماء", labelX, y)
            rightAlignString(g2d, "${formatAmount(waterChargeValue)} درهم", y, valueX)
            y += if (isReceipt) 14 else 18

            if (isReceipt && monthlyFeeValue > 0.0) {
                drawArabic(g2d, "الرسوم الشهرية", labelX, y)
                rightAlignString(g2d, "${formatAmount(monthlyFeeValue)} درهم", y, valueX)
                y += 14
            }

            if (penaltyValue > 0.0) {
                g2d.color = Color(191, 54, 12)
                drawArabic(g2d, "غرامة التأخير", labelX, y)
                rightAlignString(g2d, "${formatAmount(penaltyValue)} درهم", y, valueX)
                g2d.color = textColor
                y += if (isReceipt) 14 else 18
            }
            y += if (isReceipt) 8 else 12

            // Total
            g2d.font = getArabicFont(Font.BOLD, totalSize)
            if (isPaid) g2d.color = Color(27, 94, 32)
            drawArabic(g2d, "المجموع الكلي", labelX, y)
            rightAlignString(g2d, "${formatAmount(invoice.totalAmount)} درهم", y, valueX)
            g2d.color = textColor
            y += if (isReceipt) 25 else 35

            // Status box
            if (isPaid) {
                g2d.color = Color(27, 94, 32)
                g2d.font = getArabicFont(Font.BOLD, bodySize)
                centerString(g2d, "مدفوعة ✓ ${formatDate(invoice.issueDate)}", y, width)
                g2d.color = textColor
                y += if (isReceipt) 20 else 30
            } else if (invoice.dueDate > 0) {
                g2d.color = Color(191, 54, 12)
                g2d.font = getArabicFont(Font.BOLD, bodySize)
                centerString(g2d, "اجل الدفع: ${formatDate(invoice.dueDate)}", y, width)
                g2d.color = textColor
                y += if (isReceipt) 20 else 30
            }

            // Footer
            g2d.font = getArabicFont(Font.ITALIC, bodySize)
            drawArabicCentered(g2d, "شكرا لالتزامكم بتسديد واجباتكم", y, width)

            Printable.PAGE_EXISTS
        }
    }

    private fun createNotificationPrintable(
        invoice: Invoice,
        subscriber: Subscriber,
        settings: AppSettings
    ): Printable {
        return Printable { graphics, pageFormat, pageIndex ->
            if (pageIndex > 0) return@Printable Printable.NO_SUCH_PAGE

            val g2d = graphics as Graphics2D
            g2d.translate(pageFormat.imageableX, pageFormat.imageableY)
            val width = pageFormat.imageableWidth.toInt()

            val isReceipt = settings.printFormat == "RECEIPT" || settings.printFormat == "POS"
            val textColor = if (isReceipt) Color.BLACK else Color(50, 50, 50)

            val marginX = if (isReceipt) 10 else 40
            val contentWidth = width - marginX * 2
            var y = if (isReceipt) 20 else 40

            // Header: Association Name
            val titleSize = if (isReceipt) 14 else 20
            val bodySize = if (isReceipt) 10 else 12
            val headerSize = if (isReceipt) 12 else 18

            // Logo
            if (!settings.logoPath.isNullOrBlank()) {
                try {
                    val logoFile = File(settings.logoPath)
                    if (logoFile.exists()) {
                        val logo: BufferedImage? = ImageIO.read(logoFile)
                        if (logo != null) {
                            val logoSize = if (isReceipt) 40 else 60
                            val logoLeft = (width - logoSize) / 2
                            g2d.drawImage(logo, logoLeft, y, logoSize, logoSize, null)
                            y += logoSize + 10
                        }
                    }
                } catch (e: Exception) {
                    println("Error loading logo for print: ${e.message}")
                }
            }

            g2d.font = getArabicFont(Font.BOLD, titleSize)
            g2d.color = textColor
            centerString(g2d, settings.associationName, y, width)
            y += if (isReceipt) 18 else 25

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
            g2d.font = getArabicFont(Font.BOLD, headerSize)
            centerString(g2d, "إشعار دفع", y, width)
            y += if (isReceipt) 20 else 35

            val penaltyApplied = invoice.isPenaltyApplied == 1L
            val penaltyValue = if (penaltyApplied) settings.lateFeeAmount else 0.0

            if (isReceipt) {
                // POS/Receipt: Stack details to prevent text overlaps
                g2d.font = getArabicFont(Font.PLAIN, bodySize)
                drawArabic(g2d, "المشترك: ${subscriber.fullName}", marginX, y)
                y += 14
                drawArabic(g2d, "العداد: ${subscriber.meterNumber}", marginX, y)
                y += 14
                drawArabic(g2d, "رقم الفاتورة: ${invoice.id}", marginX, y)
                y += 14
                drawArabic(g2d, "القراءة الحالية: ${invoice.currentReading}", marginX, y)
                y += 14
                drawArabic(g2d, "القراءة السابقة: ${invoice.previousReading}", marginX, y)
                y += 14
                drawArabic(g2d, "الاستهلاك: ${invoice.consumption} م³", marginX, y)
                y += 18

                g2d.drawLine(marginX, y, width - marginX, y)
                y += 15

                g2d.font = getArabicFont(Font.BOLD, bodySize)
                drawArabic(g2d, "المبلغ المستحق:", marginX, y)
                rightAlignString(g2d, "${formatAmount(invoice.totalAmount)} درهم", y, width - marginX)
                y += 16

                if (penaltyValue > 0.0) {
                    g2d.font = getArabicFont(Font.PLAIN, bodySize)
                    g2d.color = Color(191, 54, 12)
                    drawArabic(g2d, "بما فيها غرامة التأخير:", marginX, y)
                    rightAlignString(g2d, "${formatAmount(penaltyValue)} درهم", y, width - marginX)
                    g2d.color = textColor
                    y += 16
                }

                if (invoice.dueDate > 0) {
                    y += 5
                    g2d.color = Color(191, 54, 12)
                    g2d.font = getArabicFont(Font.BOLD, bodySize)
                    centerString(g2d, "اجل الدفع: ${formatDate(invoice.dueDate)}", y, width)
                    g2d.color = textColor
                    y += 15
                }
            } else {
                // A4/A5 notification layout with rounded box
                // Lines: name, meter, invoice#, current, previous, consumption, amount, (penalty)
                val boxLines = 8 + (if (penaltyValue > 0.0) 1 else 0)
                val lineH = 22
                val boxHeight = boxLines * lineH + 30
                g2d.color = Color(255, 253, 231)
                g2d.fillRoundRect(marginX, y - 10, contentWidth, boxHeight, 12, 12)
                g2d.color = Color(245, 176, 65)
                g2d.drawRoundRect(marginX, y - 10, contentWidth, boxHeight, 12, 12)
                g2d.color = textColor

                g2d.font = getArabicFont(Font.PLAIN, 12)
                val valueX = width - marginX - 20
                // Subscriber info
                rightAlignString(g2d, "المشترك: ${subscriber.fullName}", y, valueX)
                y += lineH
                rightAlignString(g2d, "العداد: ${subscriber.meterNumber}", y, valueX)
                y += lineH
                rightAlignString(g2d, "رقم الفاتورة: ${invoice.id}", y, valueX)
                y += lineH
                // Readings and consumption
                rightAlignString(g2d, "القراءة الحالية: ${invoice.currentReading}", y, valueX)
                y += lineH
                rightAlignString(g2d, "القراءة السابقة: ${invoice.previousReading}", y, valueX)
                y += lineH
                rightAlignString(g2d, "الاستهلاك: ${invoice.consumption} م³", y, valueX)
                y += lineH + 4

                // Divider inside box
                g2d.color = Color(245, 176, 65)
                g2d.drawLine(marginX + 10, y, width - marginX - 10, y)
                g2d.color = textColor
                y += 8

                // Amount due
                g2d.color = Color(191, 54, 12)
                g2d.font = getArabicFont(Font.BOLD, 14)
                rightAlignString(g2d, "المبلغ المستحق: ${formatAmount(invoice.totalAmount)} درهم", y, valueX)
                g2d.color = textColor
                y += lineH + 4

                // Penalty breakdown (if applied)
                if (penaltyValue > 0.0) {
                    g2d.font = getArabicFont(Font.PLAIN, 11)
                    g2d.color = Color(191, 54, 12)
                    rightAlignString(g2d, "بما فيها غرامة التأخير: ${formatAmount(penaltyValue)} درهم", y, valueX)
                    g2d.color = textColor
                    y += lineH
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
                    g2d.color = textColor
                    y += 55
                }
            }

            Printable.PAGE_EXISTS
        }
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

        val basePrintable = createInvoicePrintable(items, settings)
        val printable = if (isReceipt) ImageWrapperPrintable(basePrintable) else basePrintable
        job.setPrintable(printable, pageFormat)
        if (job.printDialog()) {
            try {
                job.print()
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        } else {
            throw Exception("تم إلغاء عملية الطباعة من قبل المستخدم")
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
        val isReceipt = settings.printFormat == "RECEIPT" || settings.printFormat == "POS"
        val widthPt = if (isReceipt) (80.0 * 72.0 / 25.4) else (148.0 * 72.0 / 25.4)
        val heightPt = if (isReceipt) (300.0 * 72.0 / 25.4) else (210.0 * 72.0 / 25.4)
        paper.setSize(widthPt, heightPt)
        if (isReceipt) {
            paper.setImageableArea(8.0, 8.0, widthPt - 16.0, heightPt - 16.0)
        } else {
            paper.setImageableArea(20.0, 20.0, widthPt - 40.0, heightPt - 40.0)
        }
        pageFormat.paper = paper
        pageFormat.orientation = PageFormat.PORTRAIT

        val basePrintable = createNotificationPrintable(invoice, subscriber, settings)
        val printable = if (isReceipt) ImageWrapperPrintable(basePrintable) else basePrintable
        job.setPrintable(printable, pageFormat)
        if (job.printDialog()) {
            try {
                job.print()
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        } else {
            throw Exception("تم إلغاء عملية الطباعة من قبل المستخدم")
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

    // ── Bluetooth Printing Support on macOS (via virtual serial port /dev/tty.*) ──

    private fun renderPrintableToBitmap(printable: Printable, pageFormat: PageFormat): BufferedImage {
        val widthPx = 560
        val widthPt = pageFormat.paper.width
        val scale = widthPx / widthPt
        val heightPx = (pageFormat.paper.height * scale).toInt()

        val image = BufferedImage(widthPx, heightPx, BufferedImage.TYPE_INT_RGB)
        val g2d = image.createGraphics()
        
        // Enable antialiasing for clean receipt text/layout
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)
        
        g2d.color = Color.WHITE
        g2d.fillRect(0, 0, widthPx, heightPx)
        g2d.scale(scale, scale)

        try {
            printable.print(g2d, pageFormat, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        g2d.dispose()

        return cropImage(image)
    }

    private fun cropImage(image: BufferedImage): BufferedImage {
        var cropHeight = image.height
        for (y in image.height - 1 downTo 0) {
            var hasContent = false
            for (x in 0 until image.width) {
                val rgb = image.getRGB(x, y)
                val r = (rgb shr 16) and 0xFF
                val g = (rgb shr 8) and 0xFF
                val b = rgb and 0xFF
                // Check if the pixel is not fully white
                if (r < 240 || g < 240 || b < 240) {
                    hasContent = true
                    break
                }
            }
            if (hasContent) {
                cropHeight = y + 24
                break
            }
        }
        val finalHeight = minOf(cropHeight, image.height).coerceAtLeast(100)
        return image.getSubimage(0, 0, image.width, finalHeight)
    }

    private fun buildRasterCommand(img: BufferedImage): ByteArray {
        val w = img.width
        val h = img.height
        val widthBytes = (w + 7) / 8

        val header = byteArrayOf(
            0x1D, 0x76, 0x30,
            0.toByte(), // m = 0
            (widthBytes % 256).toByte(),
            (widthBytes / 256).toByte(),
            (h % 256).toByte(),
            (h / 256).toByte()
        )

        val rasterData = ByteArray(widthBytes * h)
        for (y in 0 until h) {
            val rowOffset = y * widthBytes
            for (x in 0 until w) {
                val rgb = img.getRGB(x, y)
                val r = (rgb shr 16) and 0xFF
                val g = (rgb shr 8) and 0xFF
                val b = rgb and 0xFF
                val gray = (r * 0.299 + g * 0.587 + b * 0.114).toInt()
                if (gray < 180) { // Black pixel threshold
                    val byteIndex = rowOffset + (x / 8)
                    val bitIndex = 7 - (x % 8)
                    rasterData[byteIndex] = (rasterData[byteIndex].toInt() or (1 shl bitIndex)).toByte()
                }
            }
        }
        return header + rasterData
    }

    private fun isMacBluetoothOn(): Boolean {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "system_profiler SPBluetoothDataType | grep \"State:\""))
            val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
            val line = reader.readLine()
            process.destroy()
            if (line != null && line.contains("Off")) {
                return false
            }
        } catch (_: Exception) {}
        return true
    }

    private suspend fun printInvoiceOrNotificationViaBluetooth(
        printable: Printable,
        deviceAddress: String
    ): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val osName = System.getProperty("os.name").lowercase()
        val isWindows = osName.contains("win")
        val isMac = osName.contains("mac")

        println("[DesktopPrintService] Starting Bluetooth print job to address: $deviceAddress")

        if (isMac) {
            println("[DesktopPrintService] Checking macOS Bluetooth power state...")
            if (!isMacBluetoothOn()) {
                println("[DesktopPrintService] ERROR: macOS Bluetooth is powered OFF.")
                return@withContext Result.failure(Exception("بلوتوث الماك غير مفعل. يرجى تفعيل البلوتوث أولاً."))
            }
            println("[DesktopPrintService] macOS Bluetooth is ON.")
        }

        val sanitizedAddress = if (deviceAddress.startsWith("/dev/tty.")) {
            deviceAddress.replace("/dev/tty.", "/dev/cu.")
        } else {
            deviceAddress
        }
        
        if (!isWindows) {
            val file = File(sanitizedAddress)
            if (!file.exists()) {
                println("[DesktopPrintService] ERROR: Device port file does not exist: $sanitizedAddress")
                return@withContext Result.failure(Exception("منفذ الطابعة غير موجود أو غير متصل: $sanitizedAddress"))
            }
        }

        val widthPt = 80.0 * 72.0 / 25.4
        val heightPt = 420.0 * 72.0 / 25.4
        val pageFormat = PageFormat().apply {
            paper = Paper().apply {
                setSize(widthPt, heightPt)
                setImageableArea(8.0, 8.0, widthPt - 16.0, heightPt - 16.0)
            }
            orientation = PageFormat.PORTRAIT
        }

        val bitmap = renderPrintableToBitmap(printable, pageFormat)
        val rasterCmd = buildRasterCommand(bitmap)

        printMutex.withLock {
            var stream: java.io.FileOutputStream? = null
            try {
                var retries = 5
                val delayMs = 1500L
                println("[DesktopPrintService] Opening file stream connection to $sanitizedAddress ...")
                while (retries > 0) {
                    try {
                        stream = if (isWindows) {
                            java.io.FileOutputStream(sanitizedAddress)
                        } else {
                            java.io.FileOutputStream(File(sanitizedAddress))
                        }
                        println("[DesktopPrintService] Connection opened successfully. Allowing RFCOMM link to settle...")
                        delay(1500)
                        break
                    } catch (e: Exception) {
                        retries--
                        println("[DesktopPrintService] Connection attempt failed (retries left: $retries): ${e.message}")
                        if (retries == 0) {
                            println("[DesktopPrintService] ERROR: All connection retries exhausted.")
                            return@withLock Result.failure(Exception("منفذ الطابعة مشغول أو غير جاهز. يرجى الانتظار والمحاولة مرة أخرى: ${e.message}"))
                        }
                        delay(delayMs)
                    }
                }

                if (stream == null) {
                    println("[DesktopPrintService] ERROR: Stream is null.")
                    return@withLock Result.failure(Exception("فشل فتح منفذ الطابعة"))
                }

                println("[DesktopPrintService] Sending ESC/POS initialization command...")
                // ESC @ (Init)
                stream.write(byteArrayOf(0x1B, 0x40))
                stream.flush()
                delay(50)

                println("[DesktopPrintService] Streaming raster image data (${rasterCmd.size} bytes)...")
                // Write in chunks to prevent printer buffer overflow
                var offset = 0
                val chunkSize = 1024
                while (offset < rasterCmd.size) {
                    val len = minOf(chunkSize, rasterCmd.size - offset)
                    stream.write(rasterCmd, offset, len)
                    stream.flush()
                    offset += len
                    delay(20)
                }

                println("[DesktopPrintService] Sending feed and cut commands...")
                // Feed + full cut (highly compatible)
                stream.write(byteArrayOf(0x1B, 0x64, 0x04))       // feed 4 lines
                stream.write(byteArrayOf(0x1D, 0x56, 0x41, 0x00)) // full cut
                stream.flush()

                println("[DesktopPrintService] Closing stream...")
                try { stream.close() } catch (_: Exception) {}
                println("[DesktopPrintService] Print job completed successfully.")
                Result.success(Unit)
            } catch (e: Exception) {
                println("[DesktopPrintService] ERROR during print: ${e.message}")
                e.printStackTrace()
                if (stream != null) {
                    try { stream.close() } catch (_: Exception) {}
                }
                Result.failure(Exception("خطأ أثناء الطباعة: ${e.message}"))
            }
        }
    }

    override suspend fun getPairedBluetoothPrinters(): List<BluetoothPrinterInfo> {
        val osName = System.getProperty("os.name").lowercase()
        if (osName.contains("win")) {
            val ports = mutableListOf<BluetoothPrinterInfo>()
            try {
                val process = Runtime.getRuntime().exec("reg query HKLM\\HARDWARE\\DEVICEMAP\\SERIALCOMM")
                val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line!!.contains("REG_SZ")) {
                        val parts = line!!.split(Regex("\\s+"))
                        val comPort = parts.lastOrNull { it.startsWith("COM", ignoreCase = true) }
                        if (comPort != null) {
                            ports.add(
                                BluetoothPrinterInfo(
                                    name = "Bluetooth Printer ($comPort)",
                                    address = comPort
                                )
                            )
                        }
                    }
                }
                process.waitFor()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (ports.isEmpty()) {
                // Fallback to COM1..COM9
                for (i in 1..9) {
                    ports.add(BluetoothPrinterInfo("Bluetooth Printer (COM$i)", "COM$i"))
                }
            }
            return ports
        }

        val devDir = File("/dev")
        if (!devDir.exists() || !devDir.isDirectory) return emptyList()

        val files = devDir.listFiles { _, name ->
            name.startsWith("cu.") && // ONLY cu. to avoid duplicate and DCD hang
            !name.contains("Bluetooth-Incoming") &&
            !name.contains("debug-console")
        } ?: return emptyList()

        return files.map { file ->
            val cleanName = file.name
                .removePrefix("tty.")
                .removePrefix("cu.")
                .replace("-", " ")
            BluetoothPrinterInfo(
                name = cleanName,
                address = file.absolutePath
            )
        }
    }

    override suspend fun printInvoiceViaBluetooth(
        invoice: Invoice,
        subscriber: Subscriber,
        settings: AppSettings,
        deviceAddress: String
    ): Result<Unit> = printInvoiceOrNotificationViaBluetooth(
        createInvoicePrintable(listOf(invoice to subscriber), settings),
        deviceAddress
    )

    override suspend fun printNotificationViaBluetooth(
        invoice: Invoice,
        subscriber: Subscriber,
        settings: AppSettings,
        deviceAddress: String
    ): Result<Unit> = printInvoiceOrNotificationViaBluetooth(
        createNotificationPrintable(invoice, subscriber, settings),
        deviceAddress
    )

    override suspend fun testBluetoothPrint(deviceAddress: String): Result<Unit> {
        val printable = Printable { graphics, pf, pageIndex ->
            if (pageIndex > 0) Printable.NO_SUCH_PAGE else {
                val g2d = graphics as Graphics2D
                g2d.translate(pf.imageableX, pf.imageableY)
                val w = pf.imageableWidth.toInt()
                var y = 20

                g2d.font = getArabicFont(Font.BOLD, 14)
                g2d.color = Color.BLACK
                centerString(g2d, "اختبار طباعة", y, w)
                y += 30
                g2d.stroke = BasicStroke(1f)
                g2d.drawLine(10, y, w - 10, y)
                y += 25
                g2d.font = getArabicFont(Font.PLAIN, 10)
                centerString(g2d, "طابعة حرارية POS 80mm", y, w)
                y += 18
                centerString(g2d, "Bluetooth Printer (macOS)", y, w)
                y += 25
                g2d.drawLine(10, y, w - 10, y)
                y += 25
                g2d.font = getArabicFont(Font.BOLD, 12)
                g2d.color = Color(46, 125, 50)
                centerString(g2d, "✓ تم الاتصال بنجاح", y, w)
                y += 25
                g2d.color = Color.BLACK
                g2d.font = getArabicFont(Font.PLAIN, 9)
                val now = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm").format(java.util.Date())
                centerString(g2d, now, y, w)
                Printable.PAGE_EXISTS
            }
        }
        return printInvoiceOrNotificationViaBluetooth(printable, deviceAddress)
    }

    // ── USB (System Printers) on Desktop ──
    // On desktop, "USB printers" maps to system-installed printers (CUPS on macOS/Linux,
    // Windows print spooler). The system print dialog handles all connection types:
    // USB, network, AirPrint, etc.

    /**
     * Returns all system-installed printers as USB printer options.
     * Uses Java's PrintServiceLookup to query the OS print spooler.
     */
    override suspend fun getConnectedUsbPrinters(): List<UsbPrinterInfo> {
        val services = PrintServiceLookup.lookupPrintServices(null, null)
        return services.map { service ->
            UsbPrinterInfo(
                deviceName = service.name,
                deviceId = service.name.hashCode(),
                vendorId = 0,
                productId = 0
            )
        }
    }

    /**
     * Prints an invoice via the system print dialog with receipt-sized (80mm) paper.
     * The deviceId (printer name hash) is used to pre-select the printer if possible.
     */
    override suspend fun printInvoiceViaUsb(
        invoice: Invoice,
        subscriber: Subscriber,
        settings: AppSettings,
        deviceId: Int
    ): Result<Unit> = try {
        printInvoices(
            items = listOf(invoice to subscriber),
            settings = settings,
            jobName = "فاتورة ${invoice.id} - ${subscriber.fullName}",
        )
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(Exception("فشل الطباعة: ${e.message}"))
    }

    /**
     * Prints a payment notification via the system print dialog.
     */
    override suspend fun printNotificationViaUsb(
        invoice: Invoice,
        subscriber: Subscriber,
        settings: AppSettings,
        deviceId: Int
    ): Result<Unit> = try {
        printNotification(invoice, subscriber, settings)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(Exception("فشل الطباعة: ${e.message}"))
    }

    /**
     * Prints a test page to verify the printer connection.
     * On desktop, this opens the system print dialog with a simple test page.
     */
    override suspend fun testUsbPrint(deviceId: Int): Result<Unit> = try {
        val job = PrinterJob.getPrinterJob()
        job.jobName = "اختبار طباعة POS"

        // Try to pre-select the printer by name
        val services = PrintServiceLookup.lookupPrintServices(null, null)
        val targetService = services.find { it.name.hashCode() == deviceId }
        if (targetService != null) {
            try { job.setPrintService(targetService) } catch (_: Exception) {}
        }

        val pageFormat = job.defaultPage().clone() as PageFormat
        val paper = Paper()
        val widthPt = 80.0 * 72.0 / 25.4
        val heightPt = 200.0 * 72.0 / 25.4
        paper.setSize(widthPt, heightPt)
        paper.setImageableArea(8.0, 8.0, widthPt - 16.0, heightPt - 16.0)
        pageFormat.paper = paper
        pageFormat.orientation = PageFormat.PORTRAIT

        val printable = Printable { graphics, pf, pageIndex ->
            if (pageIndex > 0) Printable.NO_SUCH_PAGE else {
                val g2d = graphics as Graphics2D
                g2d.translate(pf.imageableX, pf.imageableY)
                val w = pf.imageableWidth.toInt()
                var y = 20

                g2d.font = Font("Arial", Font.BOLD, 14)
                g2d.color = Color.BLACK
                centerString(g2d, "اختبار طباعة", y, w)
                y += 30
                g2d.stroke = BasicStroke(1f)
                g2d.drawLine(10, y, w - 10, y)
                y += 25
                g2d.font = Font("Arial", Font.PLAIN, 10)
                centerString(g2d, "طابعة حرارية POS 80mm", y, w)
                y += 18
                centerString(g2d, "System Printer", y, w)
                y += 25
                g2d.drawLine(10, y, w - 10, y)
                y += 25
                g2d.font = Font("Arial", Font.BOLD, 12)
                g2d.color = Color(46, 125, 50)
                centerString(g2d, "\u2713 تم الاتصال بنجاح", y, w)
                y += 25
                g2d.color = Color.BLACK
                g2d.font = Font("Arial", Font.PLAIN, 9)
                val now = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm").format(java.util.Date())
                centerString(g2d, now, y, w)
                Printable.PAGE_EXISTS
            }
        }

        job.setPrintable(printable, pageFormat)
        if (job.printDialog()) {
            job.print()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(Exception("فشل الطباعة: ${e.message}"))
    }
}
