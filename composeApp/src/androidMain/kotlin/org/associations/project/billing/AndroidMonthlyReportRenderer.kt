package org.associations.project.billing

import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
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
import java.io.File
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId

object AndroidMonthlyReportRenderer {
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

    fun render(report: MonthlyReportData, settings: AppSettings, out: OutputStream) {
        val pdf = PdfDocument(PdfWriter(out))
        val doc = Document(pdf).apply { setMargins(36f,36f,50f,36f) }
        val f = loadFont()
        val b = loadFont(true)
        header(doc, settings, f, b, report)
        summary(doc, report, f, b)
        invoices(doc, report, f, b)
        if (report.transactions.isNotEmpty()) txns(doc, report, f, b)
        doc.close()
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
        doc.add(P(s.associationName, b, 18f, C_PRI).setTextAlignment(TextAlignment.CENTER))
        if (s.associationAddress.isNotBlank()) doc.add(P(s.associationAddress, f, 11f, C_GREY).setTextAlignment(TextAlignment.CENTER))
        if (s.associationPhone.isNotBlank()) doc.add(P(s.associationPhone, f, 11f, C_GREY).setTextAlignment(TextAlignment.CENTER))
        doc.add(sep().setMarginBottom(8f))
        doc.add(P("التقرير الشهري - ${r.monthYear.displayName}", b, 16f, C_PRI).setTextAlignment(TextAlignment.CENTER).setMarginBottom(4f))
        doc.add(P("الفترة: ${fmtDate(r.monthYear.startEpochMillis)} – ${fmtDate(r.monthYear.endEpochMillis)}", f, 9f, C_GLT).setTextAlignment(TextAlignment.CENTER).setMarginBottom(8f))
        doc.add(sep().setMarginBottom(12f))
    }

    private fun summary(doc: Document, r: MonthlyReportData, f: PdfFont, b: PdfFont) {
        val t = T(floatArrayOf(1f))
        t.addCell(C("إجمالي الاستهلاك: ${r.totalConsumption} م³", f, 11f, C_GREY, C_R1))
        t.addCell(C("عدد الفواتير: ${r.totalInvoicesCount} | المبلغ: ${fmt(r.totalInvoicesAmount)} درهم", f, 11f, C_GREY, C_R1))
        t.addCell(C("الفواتير المسددة: ${r.paidInvoicesCount} | المبلغ: ${fmt(r.paidInvoicesAmount)} درهم", f, 11f, C_GREY, C_R1))
        t.addCell(C("الفواتير غير المسددة: ${r.unpaidInvoicesCount} | المبلغ: ${fmt(r.unpaidInvoicesAmount)} درهم", f, 11f, C_GREY, C_R1))
        doc.add(t.setMarginBottom(12f))
        val fin = T(floatArrayOf(1f)).setWidth(UnitValue.createPercentValue(60f)).setHorizontalAlignment(HorizontalAlignment.CENTER)
        fin.addCell(C("الإيرادات: +${fmt(r.totalIncomeAmount)} درهم", f, 11f, C_GRN, C_R1))
        fin.addCell(C("المصاريف: -${fmt(r.totalExpensesAmount)} درهم", f, 11f, C_RED, C_R1))
        fin.addCell(C("الرصيد الصافي: ${fmt(r.netBalance)} درهم", b, 11f, C_PRI, C_R1))
        doc.add(fin.setMarginBottom(16f))
    }

    private fun invoices(doc: Document, r: MonthlyReportData, f: PdfFont, b: PdfFont) {
        if (r.invoices.isEmpty()) return
        doc.add(P("الفواتير", b, 13f, C_PRI).setTextAlignment(TextAlignment.RIGHT).setMarginBottom(6f).setBaseDirection(BaseDirection.RIGHT_TO_LEFT))
        val t = T(floatArrayOf(3f, 2f, 1.5f, 2f, 1.5f))
        t.addHeaderCell(H("المشترك", b))
        t.addHeaderCell(H("العداد", b))
        t.addHeaderCell(H("الاستهلاك", b))
        t.addHeaderCell(H("المبلغ", b))
        t.addHeaderCell(H("الحالة", b))
        var i = 0
        for (inv in r.invoices) {
            val bg = if (i % 2 == 0) C_R1 else C_R2
            t.addCell(C(inv.subscriberName ?: "-", f, 10f, C_GREY, bg))
            t.addCell(C(inv.meterNumber ?: "-", f, 10f, C_GREY, bg))
            t.addCell(C("${inv.consumption} م³", f, 10f, C_GREY, bg))
            t.addCell(C("${fmt(inv.totalAmount)} DH", f, 10f, C_GREY, bg))
            val st = if (inv.status == "PAID") "مدفوعة" else "غير مدفوعة"
            val sc = if (inv.status == "PAID") C_GRN else C_RED
            t.addCell(C(st, f, 10f, sc, bg))
            i++
        }
        val tr = Cell(1, 5).add(P("المجموع الكلي: ${fmt(r.totalInvoicesAmount)} DH | ${r.totalInvoicesCount} فاتورة", b, 10f, C_BLK))
        tr.setBackgroundColor(C_AMB).setTextAlignment(TextAlignment.RIGHT).setPadding(6f).setBorder(Border.NO_BORDER)
        t.addCell(tr)
        doc.add(t.setMarginBottom(16f))
    }

    private fun txns(doc: Document, r: MonthlyReportData, f: PdfFont, b: PdfFont) {
        doc.add(P("المعاملات المالية", b, 13f, C_PRI).setTextAlignment(TextAlignment.RIGHT).setMarginBottom(6f).setBaseDirection(BaseDirection.RIGHT_TO_LEFT))
        val t = T(floatArrayOf(1.5f, 2f, 3f, 2f, 2f))
        t.addHeaderCell(H("النوع", b))
        t.addHeaderCell(H("الصنف", b))
        t.addHeaderCell(H("الوصف", b))
        t.addHeaderCell(H("المبلغ", b))
        t.addHeaderCell(H("التاريخ", b))
        var i = 0
        for (x in r.transactions) {
            val bg = if (i % 2 == 0) C_R1 else C_R2
            val ty = if (x.type == "INCOME") "إيراد" else "مصروف"
            val tc = if (x.type == "EXPENSE") C_RED else C_GRN
            t.addCell(C(ty, f, 10f, tc, bg))
            t.addCell(C(x.category, f, 10f, C_GREY, bg))
            t.addCell(C(x.description ?: "-", f, 10f, C_GREY, bg))
            t.addCell(C("${fmt(x.amount)} DH", f, 10f, C_GREY, bg))
            t.addCell(C(fmtDate(x.date), f, 10f, C_GREY, bg))
            i++
        }
        doc.add(t.setMarginBottom(16f))
    }

    private fun T(w: FloatArray): Table = Table(w).setWidth(UnitValue.createPercentValue(100f)).setBaseDirection(BaseDirection.RIGHT_TO_LEFT)
    private fun P(t: String, f: PdfFont, s: Float, c: DeviceRgb) = Paragraph(t).setFont(f).setFontSize(s).setFontColor(c)
    private fun C(t: String, f: PdfFont, s: Float, c: DeviceRgb, bg: DeviceRgb): Cell {
        val cell = Cell().add(P(t, f, s, c))
        cell.setBackgroundColor(bg).setPadding(4f).setTextAlignment(TextAlignment.RIGHT).setBorder(Border.NO_BORDER)
        return cell
    }
    private fun H(t: String, f: PdfFont): Cell {
        val c = Cell().add(P(t, f, 10f, C_PRI))
        c.setBackgroundColor(C_HDR).setPadding(4f).setTextAlignment(TextAlignment.RIGHT).setBorder(Border.NO_BORDER)
        return c
    }
    private fun sep(): Table = T(floatArrayOf(1f)).addCell(Cell().setBorder(Border.NO_BORDER).setBorderBottom(SolidBorder(C_DIV, 1f)))

    private fun loadFont(bold: Boolean = false): PdfFont {
        val candidates = listOf(
            "/system/fonts/NotoSansArabic-Regular.ttf",
            "/system/fonts/Roboto-Regular.ttf",
            "/system/fonts/DroidSansFallback.ttf"
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
}
