package org.associations.project.billing

import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.events.IEventHandler
import com.itextpdf.kernel.events.PdfDocumentEvent
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.BaseDirection
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import org.associations.project.database.AppSettings
import org.associations.project.reports.MonthlyReportData
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId

object DesktopMonthlyReportRenderer {
    private val C_PRI = DeviceRgb(25,55,95)
    private val C_GREY = DeviceRgb(80,80,90)
    private val C_GLT = DeviceRgb(158,158,158)
    private val C_AMB = DeviceRgb(255,193,7)
    private val C_GRN = DeviceRgb(46,125,50)
    private val C_RED = DeviceRgb(191,54,12)
    private val C_HDR = DeviceRgb(245,247,250)
    private val C_R1 = DeviceRgb(255,255,255)
    private val C_R2 = DeviceRgb(248,249,250)
    private val C_DIV = DeviceRgb(224,224,224)
    private val C_BLK = DeviceRgb(0,0,0)

    fun renderToPdf(report: MonthlyReportData, settings: AppSettings, out: OutputStream) {
        val pdf = PdfDocument(PdfWriter(out))
        val doc = Document(pdf).apply { setMargins(36f,36f,50f,36f) }
        val f = loadFont()
        val b = loadFont("bd")
        pdf.addEventHandler(PdfDocumentEvent.END_PAGE, Footer(f))
        header(doc, settings, f, b, report)
        summary(doc, report, f, b)
        invoices(doc, report, f, b)
        if (report.transactions.isNotEmpty()) txns(doc, report, f, b)
        doc.close()
    }

    fun renderToPdfBytes(report: MonthlyReportData, settings: AppSettings): ByteArray {
        val baos = ByteArrayOutputStream()
        renderToPdf(report, settings, baos)
        return baos.toByteArray()
    }

    private fun header(doc: Document, s: AppSettings, f: PdfFont, b: PdfFont, r: MonthlyReportData) {
        if (!s.logoPath.isNullOrBlank()) {
            try {
                val file = File(s.logoPath)
                if (file.exists()) {
                    val img = com.itextpdf.layout.element.Image(com.itextpdf.io.image.ImageDataFactory.create(file.absolutePath))
                    img.setWidth(80f).setHeight(80f).setTextAlignment(TextAlignment.CENTER)
                    doc.add(img)
                }
            } catch (_: Exception) {}
        }
        doc.add(txt(s.associationName, b, 18f, C_PRI).setTextAlignment(TextAlignment.CENTER))
        if (s.associationAddress.isNotBlank()) doc.add(txt(s.associationAddress, f, 11f, C_GREY).setTextAlignment(TextAlignment.CENTER))
        if (s.associationPhone.isNotBlank()) doc.add(txt(s.associationPhone, f, 11f, C_GREY).setTextAlignment(TextAlignment.CENTER))
        doc.add(sep().setMarginBottom(8f))
        doc.add(txt("التقرير الشهري - ${r.monthYear.displayName}", b, 16f, C_PRI).setTextAlignment(TextAlignment.CENTER).setMarginBottom(4f))
        doc.add(txt("الفترة: ${fmtDate(r.monthYear.startEpochMillis)} – ${fmtDate(r.monthYear.endEpochMillis)}", f, 9f, C_GLT).setTextAlignment(TextAlignment.CENTER).setMarginBottom(8f))
        doc.add(sep().setMarginBottom(12f))
    }

    private fun summary(doc: Document, r: MonthlyReportData, f: PdfFont, b: PdfFont) {
        val t = tbl(floatArrayOf(1f))
        t.addCell(cell("إجمالي الاستهلاك: ${r.totalConsumption} م³", f, 11f, C_GREY, C_R1))
        t.addCell(cell("عدد الفواتير: ${r.totalInvoicesCount} | المبلغ: ${fmt(r.totalInvoicesAmount)} درهم", f, 11f, C_GREY, C_R1))
        t.addCell(cell("الفواتير المسددة: ${r.paidInvoicesCount} | المبلغ: ${fmt(r.paidInvoicesAmount)} درهم", f, 11f, C_GREY, C_R1))
        t.addCell(cell("الفواتير غير المسددة: ${r.unpaidInvoicesCount} | المبلغ: ${fmt(r.unpaidInvoicesAmount)} درهم", f, 11f, C_GREY, C_R1))
        doc.add(t.setMarginBottom(12f))
        val fin = tbl(floatArrayOf(1f)).setWidth(UnitValue.createPercentValue(60f)).setHorizontalAlignment(HorizontalAlignment.CENTER)
        fin.addCell(cell("الإيرادات: +${fmt(r.totalIncomeAmount)} درهم", f, 11f, C_GRN, C_R1))
        fin.addCell(cell("المصاريف: -${fmt(r.totalExpensesAmount)} درهم", f, 11f, C_RED, C_R1))
        fin.addCell(cell("الرصيد الصافي: ${fmt(r.netBalance)} درهم", b, 11f, C_PRI, C_R1))
        doc.add(fin.setMarginBottom(16f))
    }

    private fun invoices(doc: Document, r: MonthlyReportData, f: PdfFont, b: PdfFont) {
        if (r.invoices.isEmpty()) return
        doc.add(txt("الفواتير", b, 13f, C_PRI).setTextAlignment(TextAlignment.RIGHT).setMarginBottom(6f).setBaseDirection(BaseDirection.RIGHT_TO_LEFT))
        val t = tbl(floatArrayOf(3f, 2f, 1.5f, 2f, 1.5f))
        t.addHeaderCell(hcell("المشترك", b))
        t.addHeaderCell(hcell("العداد", b))
        t.addHeaderCell(hcell("الاستهلاك", b))
        t.addHeaderCell(hcell("المبلغ", b))
        t.addHeaderCell(hcell("الحالة", b))
        var i = 0
        for (inv in r.invoices) {
            val bg = if (i % 2 == 0) C_R1 else C_R2
            t.addCell(cell(inv.subscriberName ?: "-", f, 10f, C_GREY, bg))
            t.addCell(cell(inv.meterNumber ?: "-", f, 10f, C_GREY, bg))
            t.addCell(cell("${inv.consumption} م³", f, 10f, C_GREY, bg))
            t.addCell(cell("${fmt(inv.totalAmount)} DH", f, 10f, C_GREY, bg))
            val st = if (inv.status == "PAID") "مدفوعة" else "غير مدفوعة"
            val sc = if (inv.status == "PAID") C_GRN else C_RED
            t.addCell(cell(st, f, 10f, sc, bg))
            i++
        }
        val tr = Cell(1, 5).add(txt("المجموع الكلي: ${fmt(r.totalInvoicesAmount)} DH | ${r.totalInvoicesCount} فاتورة", b, 10f, C_BLK))
        tr.setBackgroundColor(C_AMB).setTextAlignment(TextAlignment.RIGHT).setPadding(6f).setBorder(Border.NO_BORDER)
        t.addCell(tr)
        doc.add(t.setMarginBottom(16f))
    }

    private fun txns(doc: Document, r: MonthlyReportData, f: PdfFont, b: PdfFont) {
        doc.add(txt("المعاملات المالية", b, 13f, C_PRI).setTextAlignment(TextAlignment.RIGHT).setMarginBottom(6f).setBaseDirection(BaseDirection.RIGHT_TO_LEFT))
        val t = tbl(floatArrayOf(1.5f, 2f, 3f, 2f, 2f))
        t.addHeaderCell(hcell("النوع", b))
        t.addHeaderCell(hcell("الصنف", b))
        t.addHeaderCell(hcell("الوصف", b))
        t.addHeaderCell(hcell("المبلغ", b))
        t.addHeaderCell(hcell("التاريخ", b))
        var i = 0
        for (x in r.transactions) {
            val bg = if (i % 2 == 0) C_R1 else C_R2
            val ty = if (x.type == "INCOME") "إيراد" else "مصروف"
            val tc = if (x.type == "EXPENSE") C_RED else C_GRN
            t.addCell(cell(ty, f, 10f, tc, bg))
            t.addCell(cell(x.category, f, 10f, C_GREY, bg))
            t.addCell(cell(x.description ?: "-", f, 10f, C_GREY, bg))
            t.addCell(cell("${fmt(x.amount)} DH", f, 10f, C_GREY, bg))
            t.addCell(cell(fmtDate(x.date), f, 10f, C_GREY, bg))
            i++
        }
        doc.add(t.setMarginBottom(16f))
    }

    private fun tbl(w: FloatArray): Table = Table(w).setWidth(UnitValue.createPercentValue(100f)).setBaseDirection(BaseDirection.RIGHT_TO_LEFT)
    private fun txt(t: String, f: PdfFont, s: Float, c: DeviceRgb) = Paragraph(t).setFont(f).setFontSize(s).setFontColor(c)
    private fun cell(t: String, f: PdfFont, s: Float, c: DeviceRgb, bg: DeviceRgb): Cell {
        val cell = Cell().add(txt(t, f, s, c))
        cell.setBackgroundColor(bg).setPadding(4f).setTextAlignment(TextAlignment.RIGHT).setBorder(Border.NO_BORDER)
        return cell
    }
    private fun hcell(t: String, f: PdfFont): Cell {
        val c = Cell().add(txt(t, f, 10f, C_PRI))
        c.setBackgroundColor(C_HDR).setPadding(4f).setTextAlignment(TextAlignment.RIGHT).setBorder(Border.NO_BORDER)
        return c
    }
    private fun sep(): Table = tbl(floatArrayOf(1f)).addCell(Cell().setBorder(Border.NO_BORDER).setBorderBottom(SolidBorder(C_DIV, 1f)))

    private fun loadFont(style: String = ""): PdfFont {
        val candidates = listOf(
            "C:\\Windows\\Fonts\\tahoma$style.ttf",
            "C:\\Windows\\Fonts\\arial$style.ttf",
            "C:\\Windows\\Fonts\\segoeui$style.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/Library/Fonts/Arial.ttf"
        )
        for (path in candidates) {
            try {
                val file = File(path)
                if (file.exists()) return PdfFontFactory.createFont(path, "Identity-H")
            } catch (_: Exception) {}
        }
        return PdfFontFactory.createFont()
    }

    private fun fmt(v: Double): String = if (v == v.toLong().toDouble()) "${v.toLong()}" else String.format("%.2f", v)
    private fun fmtDate(epoch: Long): String {
        if (epoch <= 0) return "-"
        val d = Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).toLocalDate()
        return String.format("%02d/%02d/%d", d.dayOfMonth, d.monthValue, d.year)
    }

    private class Footer(private val font: PdfFont) : IEventHandler {
        override fun handleEvent(event: com.itextpdf.kernel.events.Event) {
            val ev = event as PdfDocumentEvent
            val page = ev.page
            val canvas = PdfCanvas(page)
            val pageNum = ev.document.getPageNumber(page)
            val size = page.pageSize
            canvas.beginText().setFontAndSize(font, 9f)
            val text = "صفحة $pageNum"
            val tw = font.getWidth(text, 9f)
            canvas.moveText(((size.width - tw) / 2f).toDouble(), 30.0).showText(text).endText()
            val now = java.time.LocalDateTime.now()
            val ts = String.format("تم الإنشاء: %02d/%02d/%d %02d:%02d", now.dayOfMonth, now.monthValue, now.year, now.hour, now.minute)
            canvas.beginText().setFontAndSize(font, 8f)
            canvas.moveText(36.0, 20.0).showText(ts).endText()
        }
    }
}
