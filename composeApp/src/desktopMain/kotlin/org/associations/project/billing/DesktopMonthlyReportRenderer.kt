package org.associations.project.billing

import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.geom.Rectangle
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import org.associations.project.database.AppSettings
import org.associations.project.reports.MonthlyReportData
import org.associations.project.database.GetAllInvoicesByMonth
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import javax.imageio.ImageIO

/**
 * Renders the monthly report into A4-sized BufferedImage pages using Java Graphics2D
 * and assembles them into a PDF via the iText kernel. Java Graphics2D performs Arabic
 * shaping natively when the chosen Font supports Arabic.
 */
object DesktopMonthlyReportRenderer {

    // A4 @ ~150 dpi
    private const val PAGE_W = 1240
    private const val PAGE_H = 1754
    private const val MARGIN = 60f
    private const val CONTENT_W = PAGE_W - MARGIN * 2
    private const val FOOTER_H = 60f
    private const val SAFE_BOTTOM = PAGE_H - MARGIN - FOOTER_H

    // ---- Theme colours (Material-inspired) ----
    private val C_PRIMARY       = Color(25, 55, 95)      // deep navy header
    private val C_PRIMARY_LIGHT = Color(66, 133, 244)    // blue accent
    private val C_BG_PAGE     = Color(250, 250, 252)    // very light grey page bg
    private val C_CARD_BG     = Color(255, 255, 255)    // white card
    private val C_CARD_BORDER = Color(220, 220, 230)    // light border
    private val C_SHADOW      = Color(0, 0, 0, 18)      // subtle shadow alpha
    private val C_AMBER       = Color(255, 193, 7)       // amber/gold totals
    private val C_GREEN       = Color(46, 125, 50)       // income/success
    private val C_RED         = Color(191, 54, 12)        // expense/danger
    private val C_GREY_TEXT   = Color(80, 80, 90)        // body text
    private val C_GREY_LIGHT  = Color(158, 158, 158)     // muted text
    private val C_TABLE_HDR   = Color(245, 247, 250)     // table header bg
    private val C_TABLE_ROW1  = Color(255, 255, 255)     // zebra 1
    private val C_TABLE_ROW2  = Color(248, 249, 250)     // zebra 2
    private val C_DIVIDER     = Color(224, 224, 224)     // divider line

    fun renderToPdf(report: MonthlyReportData, settings: AppSettings, out: OutputStream) {
        val pages = ReportPageBuilder(report, settings).buildPages()
        writePdf(pages, out)
    }

    fun renderToPdfBytes(report: MonthlyReportData, settings: AppSettings): ByteArray {
        val baos = ByteArrayOutputStream()
        renderToPdf(report, settings, baos)
        return baos.toByteArray()
    }

    // ============================================================
    //  Page Builder – tracks current page, handles breaks cleanly
    // ============================================================
    private class ReportPageBuilder(
        private val report: MonthlyReportData,
        private val settings: AppSettings
    ) {
        private val pages = mutableListOf<BufferedImage>()
        private var currentPage: BufferedImage? = null
        private var g: Graphics2D? = null
        private var yCursor = 0f

        // Fonts (created once)
        private val fTitle    = arabicFont(Font.BOLD, 30)
        private val fSub      = arabicFont(Font.BOLD, 22)
        private val fBody     = arabicFont(Font.PLAIN, 16)
        private val fBodyB    = arabicFont(Font.BOLD, 16)
        private val fSmall    = arabicFont(Font.PLAIN, 12)
        private val fTblHead  = arabicFont(Font.BOLD, 14)
        private val fTblCell  = arabicFont(Font.PLAIN, 14)
        private val fFooter   = arabicFont(Font.PLAIN, 11)

        private val rightX  = PAGE_W - MARGIN
        private val leftX   = MARGIN

        fun buildPages(): List<BufferedImage> {
            startNewPage()
            drawHeader()
            drawReportTitle()
            drawSummaryCard()
            drawFinancialCard()
            drawInvoiceTable()
            if (report.transactions.isNotEmpty()) {
                drawTransactionTable()
            }
            addFooters()
            return pages
        }

        // ---- pagination helpers ----
        private fun spaceLeft() = SAFE_BOTTOM - yCursor
        private fun needSpace(h: Float): Boolean = spaceLeft() >= h
        private fun ensureSpace(h: Float) {
            if (!needSpace(h)) startNewPage()
        }

        private fun startNewPage() {
            // close previous page footer area if any
            g?.dispose()
            val bmp = BufferedImage(PAGE_W, PAGE_H, BufferedImage.TYPE_INT_RGB)
            val gg = bmp.createGraphics()
            applyHints(gg)
            // page background
            gg.color = C_BG_PAGE
            gg.fillRect(0, 0, PAGE_W, PAGE_H)
            // subtle top accent bar
            gg.color = C_PRIMARY
            gg.fillRect(0, 0, PAGE_W, 6)
            gg.color = Color.BLACK
            currentPage = bmp
            g = gg
            pages.add(bmp)
            yCursor = MARGIN + 8f  // leave room for accent bar
        }

        // ---- sections ----

        private fun drawHeader() {
            val gg = g!!
            // Logo
            if (!settings.logoPath.isNullOrBlank()) {
                try {
                    val f = File(settings.logoPath)
                    if (f.exists()) {
                        val logo = ImageIO.read(f)
                        if (logo != null) {
                            val size = 100
                            val left = (PAGE_W - size) / 2
                            gg.drawImage(logo, left, yCursor.toInt(), size, size, null)
                            yCursor += size + 16f
                        }
                    }
                } catch (_: Exception) { }
            }
            // Association name
            gg.color = C_PRIMARY
            gg.font = fTitle
            drawCentered(gg, settings.associationName, yCursor)
            yCursor += 38f
            // Address / phone
            gg.color = C_GREY_TEXT
            gg.font = fBody
            if (settings.associationAddress.isNotBlank()) {
                drawCentered(gg, settings.associationAddress, yCursor); yCursor += 22f
            }
            if (settings.associationPhone.isNotBlank()) {
                drawCentered(gg, settings.associationPhone, yCursor); yCursor += 22f
            }
            yCursor += 10f
            // divider
            drawDivider()
            yCursor += 14f
        }

        private fun drawReportTitle() {
            val gg = g!!
            gg.color = C_PRIMARY
            gg.font = fSub
            drawCentered(gg, "التقرير الشهري - ${report.monthYear.displayName}", yCursor)
            yCursor += 32f
            // date range line
            gg.color = C_GREY_LIGHT
            gg.font = fSmall
            val rangeText = "الفترة: ${formatDate(report.monthYear.startEpochMillis)}  –  ${formatDate(report.monthYear.endEpochMillis)}"
            drawCentered(gg, rangeText, yCursor)
            yCursor += 26f
            drawDivider()
            yCursor += 14f
        }

        private fun drawSummaryCard() {
            ensureSpace(180f)
            val gg = g!!
            val cardTop = yCursor
            val lines = listOf(
                "إجمالي الاستهلاك: ${report.totalConsumption} م³",
                "عدد الفواتير: ${report.totalInvoicesCount}  |  المبلغ: ${formatAmount(report.totalInvoicesAmount)} درهم",
                "الفواتير المسددة: ${report.paidInvoicesCount}  |  المبلغ: ${formatAmount(report.paidInvoicesAmount)} درهم",
                "الفواتير غير المسددة: ${report.unpaidInvoicesCount}  |  المبلغ: ${formatAmount(report.unpaidInvoicesAmount)} درهم",
            )
            val lineH = 28f
            val pad = 18f
            val cardH = lines.size * lineH + pad * 2 + 4f

            drawCardBackground(cardTop, cardH)

            gg.color = C_GREY_TEXT
            gg.font = fBody
            var ty = cardTop + pad + lineH - 4f
            for (line in lines) {
                drawRightAligned(gg, line, rightX - pad, ty)
                ty += lineH
            }
            yCursor = cardTop + cardH + 18f
        }

        private fun drawFinancialCard() {
            ensureSpace(160f)
            val gg = g!!
            val cardTop = yCursor
            val lines = listOf(
                Triple("الإيرادات:", "+${formatAmount(report.totalIncomeAmount)} درهم", C_GREEN),
                Triple("المصاريف:", "-${formatAmount(report.totalExpensesAmount)} درهم", C_RED),
                Triple("الرصيد الصافي:", "${formatAmount(report.netBalance)} درهم", C_PRIMARY),
            )
            val lineH = 28f
            val pad = 18f
            val cardH = lines.size * lineH + pad * 2 + 4f

            drawCardBackground(cardTop, cardH)

            gg.font = fBody
            var ty = cardTop + pad + lineH - 4f
            for ((label, value, col) in lines) {
                gg.color = C_GREY_TEXT
                drawRightAligned(gg, label, rightX - pad - 140f, ty) // label left of value
                gg.color = col
                gg.font = fBodyB
                drawRightAligned(gg, value, rightX - pad, ty)
                gg.font = fBody
                ty += lineH
            }
            yCursor = cardTop + cardH + 18f
        }

        // ---- invoices table ----

        private fun drawInvoiceTable() {
            if (report.invoices.isEmpty()) return
            val gg = g!!

            // Section title
            ensureSpace(50f)
            gg.color = C_PRIMARY
            gg.font = fSub
            drawRightAligned(gg, "الفواتير", rightX, yCursor)
            yCursor += 28f
            drawDivider()
            yCursor += 10f

            // Column positions (measured from right for RTL)
            val colW = CONTENT_W / 5f
            val colStatX = rightX
            val colAmtX  = rightX - colW
            val colConsX = rightX - colW * 2
            val colMeterX= rightX - colW * 3
            val colSubX  = rightX - colW * 4

            // Table header
            ensureSpace(40f)
            drawTableHeaderRow(yCursor, C_TABLE_HDR) { cy ->
                gg.color = C_PRIMARY
                gg.font = fTblHead
                drawCellRight(gg, "الحالة", colStatX, colW, cy)
                drawCellRight(gg, "المبلغ", colAmtX, colW, cy)
                drawCellRight(gg, "الاستهلاك", colConsX, colW, cy)
                drawCellRight(gg, "العداد", colMeterX, colW, cy)
                drawCellRight(gg, "المشترك", colSubX, colW, cy)
            }
            yCursor += 36f
            drawThinLine(yCursor)
            yCursor += 10f

            // Rows
            var rowIndex = 0
            for (inv in report.invoices) {
                ensureSpace(32f)
                val rowBg = if (rowIndex % 2 == 0) C_TABLE_ROW1 else C_TABLE_ROW2
                drawTableRowBackground(yCursor, 30f, rowBg)
                gg.font = fTblCell
                val cy = yCursor + 22f
                val statusText = if (inv.status == "PAID") "مدفوعة" else "غير مدفوعة"
                val statusCol = if (inv.status == "PAID") C_GREEN else C_RED
                gg.color = statusCol
                drawCellRight(gg, statusText, colStatX, colW, cy)
                gg.color = C_GREY_TEXT
                drawCellRight(gg, "${formatAmount(inv.totalAmount)} DH", colAmtX, colW, cy)
                drawCellRight(gg, "${inv.consumption} م³", colConsX, colW, cy)
                drawCellRight(gg, inv.meterNumber ?: "", colMeterX, colW, cy)
                drawCellRight(gg, inv.subscriberName ?: "", colSubX, colW, cy)
                yCursor += 30f
                rowIndex++
            }

            // Totals row
            ensureSpace(40f)
            val totalBg = C_AMBER
            val totalY = yCursor
            gg.color = totalBg
            gg.fillRoundRect(MARGIN.toInt(), totalY.toInt(), CONTENT_W.toInt(), 38, 8, 8)
            gg.color = Color.BLACK
            gg.font = fBodyB
            val ty = totalY + 26f
            drawCellRight(gg, "${report.totalInvoicesCount}", colStatX, colW, ty)
            drawCellRight(gg, "${formatAmount(report.totalInvoicesAmount)} DH", colAmtX, colW, ty)
            drawCellRight(gg, "", colConsX, colW, ty)
            drawCellRight(gg, "", colMeterX, colW, ty)
            drawCellRight(gg, "المجموع الكلي", colSubX, colW, ty)
            yCursor = totalY + 46f
        }

        // ---- transactions table ----

        private fun drawTransactionTable() {
            val gg = g!!
            ensureSpace(50f)
            gg.color = C_PRIMARY
            gg.font = fSub
            drawRightAligned(gg, "المعاملات المالية", rightX, yCursor)
            yCursor += 28f
            drawDivider()
            yCursor += 10f

            val colW = CONTENT_W / 5f
            val colDateX = rightX
            val colAmtX  = rightX - colW
            val colDescX = rightX - colW * 2
            val colCatX  = rightX - colW * 3
            val colTypeX = rightX - colW * 4

            ensureSpace(40f)
            drawTableHeaderRow(yCursor, C_TABLE_HDR) { cy ->
                gg.color = C_PRIMARY
                gg.font = fTblHead
                drawCellRight(gg, "التاريخ", colDateX, colW, cy)
                drawCellRight(gg, "المبلغ", colAmtX, colW, cy)
                drawCellRight(gg, "الوصف", colDescX, colW, cy)
                drawCellRight(gg, "الصنف", colCatX, colW, cy)
                drawCellRight(gg, "النوع", colTypeX, colW, cy)
            }
            yCursor += 36f
            drawThinLine(yCursor)
            yCursor += 10f

            var rowIndex = 0
            for (txn in report.transactions) {
                ensureSpace(32f)
                val rowBg = if (rowIndex % 2 == 0) C_TABLE_ROW1 else C_TABLE_ROW2
                drawTableRowBackground(yCursor, 30f, rowBg)
                val cy = yCursor + 22f
                val typeText = if (txn.type == "INCOME") "إيراد" else "مصروف"
                val typeCol = if (txn.type == "EXPENSE") C_RED else C_GREEN
                gg.color = typeCol
                gg.font = fTblCell
                drawCellRight(gg, typeText, colTypeX, colW, cy)
                gg.color = C_GREY_TEXT
                drawCellRight(gg, txn.category, colCatX, colW, cy)
                drawCellRight(gg, txn.description ?: "", colDescX, colW, cy)
                drawCellRight(gg, "${formatAmount(txn.amount)} DH", colAmtX, colW, cy)
                drawCellRight(gg, formatDate(txn.date), colDateX, colW, cy)
                yCursor += 30f
                rowIndex++
            }
            yCursor += 10f
        }

        // ---- footer ----

        private fun addFooters() {
            val total = pages.size
            for ((idx, bmp) in pages.withIndex()) {
                val gg = bmp.createGraphics()
                applyHints(gg)
                // footer line
                gg.color = C_DIVIDER
                gg.stroke = BasicStroke(1f)
                val footY = PAGE_H - 42
                gg.drawLine(MARGIN.toInt(), footY, (PAGE_W - MARGIN).toInt(), footY)
                // page number
                gg.color = C_GREY_LIGHT
                gg.font = fFooter
                drawCentered(gg, "صفحة ${idx + 1} / $total", footY.toFloat() + 18f)
                // generation timestamp
                val now = java.time.LocalDateTime.now()
                val ts = String.format("تم إنشاء هذا التقرير: %02d/%02d/%d  %02d:%02d",
                    now.dayOfMonth, now.monthValue, now.year, now.hour, now.minute)
                val tw = gg.fontMetrics.stringWidth(ts)
                gg.drawString(ts, MARGIN, footY.toFloat() + 18f)
                gg.dispose()
            }
        }

        // ---- drawing primitives ----

        private fun drawCardBackground(top: Float, height: Float) {
            val gg = g!!
            val r = 10
            // shadow
            gg.color = C_SHADOW
            gg.fillRoundRect((MARGIN + 2).toInt(), (top + 2).toInt(), CONTENT_W.toInt(), height.toInt(), r, r)
            // card
            gg.color = C_CARD_BG
            gg.fillRoundRect(MARGIN.toInt(), top.toInt(), CONTENT_W.toInt(), height.toInt(), r, r)
            // border
            gg.color = C_CARD_BORDER
            gg.stroke = BasicStroke(1f)
            gg.drawRoundRect(MARGIN.toInt(), top.toInt(), CONTENT_W.toInt(), height.toInt(), r, r)
        }

        private fun drawTableHeaderRow(yPos: Float, bg: Color, drawContent: (Float) -> Unit) {
            val gg = g!!
            gg.color = bg
            gg.fillRoundRect(MARGIN.toInt(), yPos.toInt(), CONTENT_W.toInt(), 34, 6, 6)
            drawContent(yPos + 24f)
        }

        private fun drawTableRowBackground(yPos: Float, h: Float, bg: Color) {
            val gg = g!!
            gg.color = bg
            gg.fillRect(MARGIN.toInt(), yPos.toInt(), CONTENT_W.toInt(), h.toInt())
        }

        private fun drawDivider() {
            val gg = g!!
            gg.color = C_DIVIDER
            gg.stroke = BasicStroke(1.5f)
            gg.drawLine(MARGIN.toInt(), yCursor.toInt(), (PAGE_W - MARGIN).toInt(), yCursor.toInt())
        }

        private fun drawThinLine(y: Float) {
            val gg = g!!
            gg.color = C_DIVIDER
            gg.stroke = BasicStroke(0.8f)
            gg.drawLine(MARGIN.toInt(), y.toInt(), (PAGE_W - MARGIN).toInt(), y.toInt())
        }

        private fun drawCellRight(gg: Graphics2D, text: String, rightX: Float, cellW: Float, y: Float) {
            val pad = 8f
            val maxW = cellW - pad * 2
            val fm = gg.fontMetrics
            var display = text
            // simple truncation if too long
            if (fm.stringWidth(text) > maxW) {
                var s = text
                while (s.isNotEmpty() && fm.stringWidth("$s…") > maxW) {
                    s = s.dropLast(1)
                }
                display = "$s…"
            }
            val tw = fm.stringWidth(display)
            gg.drawString(display, rightX - pad - tw, y)
        }
    }

    // ---------------- PDF assembly ----------------

    private fun writePdf(pages: List<BufferedImage>, out: OutputStream) {
        val pdfDoc = PdfDocument(PdfWriter(out))
        try {
            val pageSize = PageSize.A4
            for (img in pages) {
                val baos = ByteArrayOutputStream()
                ImageIO.write(img, "png", baos)
                val imgData = ImageDataFactory.create(baos.toByteArray())
                val page = pdfDoc.addNewPage(pageSize)
                val canvas = PdfCanvas(page)
                // fit image to page while preserving aspect ratio
                val scale = minOf(pageSize.width / imgData.width, pageSize.height / imgData.height)
                val w = imgData.width * scale
                val h = imgData.height * scale
                val x = (pageSize.width - w) / 2f
                val y = (pageSize.height - h) / 2f
                canvas.addImageFittedIntoRectangle(imgData, Rectangle(x, y, w, h), false)
            }
        } finally {
            pdfDoc.close()
        }
    }

    // ---------------- Helpers ----------------

    private fun applyHints(g: Graphics2D) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
    }

    private fun arabicFont(style: Int, size: Int): Font {
        val candidates = listOf(
            "Geeza Pro", "Al Bayan", "Damascus",
            "Tahoma", "Arial", "Segoe UI",
            "Noto Sans Arabic", "Noto Naskh Arabic", "Droid Sans Arabic"
        )
        for (family in candidates) {
            try {
                val font = Font(family, style, size)
                if (font.canDisplay('ا') && font.canDisplay('م')) return font
            } catch (_: Exception) { }
        }
        return Font("Dialog", style, size)
    }

    private fun drawCentered(g: Graphics2D, text: String, y: Float) {
        val w = g.fontMetrics.stringWidth(text)
        g.drawString(text, ((PAGE_W - w) / 2f), y)
    }

    private fun drawRightAligned(g: Graphics2D, text: String, rightX: Float, y: Float) {
        val w = g.fontMetrics.stringWidth(text)
        g.drawString(text, rightX - w, y)
    }

    private fun formatAmount(value: Double): String {
        return if (value == value.toLong().toDouble()) "${value.toLong()}"
        else String.format("%.2f", value)
    }

    private fun formatDate(epochMillis: Long): String {
        if (epochMillis <= 0) return "-"
        val d = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        return String.format("%02d/%02d/%d", d.dayOfMonth, d.monthValue, d.year)
    }
}
