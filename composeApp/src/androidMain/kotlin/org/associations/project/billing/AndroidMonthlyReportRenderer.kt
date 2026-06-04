package org.associations.project.billing

import com.itextpdf.html2pdf.ConverterProperties
import com.itextpdf.html2pdf.HtmlConverter
import com.itextpdf.layout.font.FontProvider
import org.associations.project.database.AppSettings
import org.associations.project.utils.ArabicShaper
import org.associations.project.reports.MonthlyReportData
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId

object AndroidMonthlyReportRenderer {

    fun render(report: MonthlyReportData, settings: AppSettings, out: OutputStream) {
        val html = buildHtml(report, settings)
        HtmlConverter.convertToPdf(html, out, buildConverterProperties())
    }

    private fun buildConverterProperties(): ConverterProperties {
        val provider = FontProvider()
        for (res in listOf("/fonts/Amiri-Regular.ttf", "/fonts/Amiri-Bold.ttf", "/fonts/Tajawal-Regular.ttf", "/fonts/Tajawal-Bold.ttf")) {
            try {
                val bytes = AndroidMonthlyReportRenderer::class.java.getResourceAsStream(res)?.readBytes()
                if (bytes != null) provider.addFont(bytes)
            } catch (_: Exception) {}
        }
        return ConverterProperties().setFontProvider(provider)
    }

    private fun buildHtml(r: MonthlyReportData, s: AppSettings): String = buildString {
        appendLine("<!DOCTYPE html>")
        appendLine("<html dir='rtl' lang='ar'>")
        appendLine("<head><meta charset='UTF-8'>")
        appendLine("<style>")
        appendLine("body { font-family: 'Amiri', 'Tajawal', sans-serif; direction: ltr; unicode-bidi: bidi-override; text-align: right; color: #333; margin: 30px; font-size: 11pt; }")
        appendLine("h1 { color: #19375f; font-size: 18pt; text-align: center; margin: 4px 0; }")
        appendLine("h2 { color: #19375f; font-size: 14pt; margin: 12px 0 6px; }")
        appendLine(".sub { color: #50505a; font-size: 11pt; text-align: center; margin: 2px 0; }")
        appendLine(".light { color: #9e9e9e; font-size: 9pt; text-align: center; margin: 2px 0; }")
        appendLine("hr { border: none; border-top: 1px solid #e0e0e0; margin: 8px 0; }")
        appendLine("table { width: 100%; border-collapse: collapse; direction: ltr; }")
        appendLine("th { background: #f5f7fa; color: #19375f; padding: 6px 8px; text-align: right; font-size: 10pt; border: none; }")
        appendLine("td { padding: 5px 8px; text-align: right; font-size: 10pt; border: none; }")
        appendLine("tr.even { background: #ffffff; }")
        appendLine("tr.odd { background: #f8f9fa; }")
        appendLine(".summary-table { margin-bottom: 12px; }")
        appendLine(".summary-table td { padding: 6px 10px; font-size: 11pt; }")
        appendLine(".fin-table { width: 60%; margin: 0 auto 16px; }")
        appendLine(".green { color: #2e7d32; }")
        appendLine(".red { color: #bf360c; }")
        appendLine(".primary { color: #19375f; }")
        appendLine(".amber-row td { background: #ffc107; font-weight: bold; padding: 8px; }")
        appendLine(".footer { font-size: 8pt; color: #999; text-align: center; margin-top: 20px; }")
        appendLine("</style>")
        appendLine("</head><body>")

        // Header
        if (!s.logoPath.isNullOrBlank()) {
            try {
                val file = java.io.File(s.logoPath)
                if (file.exists()) {
                    appendLine("<div style='text-align:center'><img src='file://${file.absolutePath}' style='width:80px;height:80px'/></div>")
                }
            } catch (_: Exception) {}
        }
        appendLine("<h1>${e(s.associationName)}</h1>")
        if (s.associationAddress.isNotBlank()) appendLine("<div class='sub'>${e(s.associationAddress)}</div>")
        if (s.associationPhone.isNotBlank()) appendLine("<div class='sub'>${e(s.associationPhone)}</div>")
        appendLine("<hr/>")
        appendLine("<h1>${ar("التقرير الشهري - ${r.monthYear.displayName}")}</h1>")
        appendLine("<div class='light'>${ar("الفترة: ${fmtDate(r.monthYear.startEpochMillis)} – ${fmtDate(r.monthYear.endEpochMillis)}")}</div>")
        appendLine("<hr/>")

        // Summary
        appendLine("<table class='summary-table'>")
        appendLine("<tr class='even'><td>${ar("إجمالي الاستهلاك: ${r.totalConsumption} م³")}</td></tr>")
        appendLine("<tr class='odd'><td>${ar("عدد الفواتير: ${r.totalInvoicesCount} | المبلغ: ${fmt(r.totalInvoicesAmount)} درهم")}</td></tr>")
        appendLine("<tr class='even'><td>${ar("الفواتير المسددة: ${r.paidInvoicesCount} | المبلغ: ${fmt(r.paidInvoicesAmount)} درهم")}</td></tr>")
        appendLine("<tr class='odd'><td>${ar("الفواتير غير المسددة: ${r.unpaidInvoicesCount} | المبلغ: ${fmt(r.unpaidInvoicesAmount)} درهم")}</td></tr>")
        appendLine("</table>")

        // Financial summary
        appendLine("<table class='fin-table'>")
        appendLine("<tr class='even'><td class='green'>${ar("الإيرادات: +${fmt(r.totalIncomeAmount)} درهم")}</td></tr>")
        appendLine("<tr class='odd'><td class='red'>${ar("المصاريف: -${fmt(r.totalExpensesAmount)} درهم")}</td></tr>")
        appendLine("<tr class='even'><td class='primary'><b>${ar("الرصيد الصافي: ${fmt(r.netBalance)} درهم")}</b></td></tr>")
        appendLine("</table>")

        // Invoices
        if (r.invoices.isNotEmpty()) {
            appendLine("<h2>${ar("الفواتير")}</h2>")
            appendLine("<table>")
            appendLine("<thead><tr><th>${ar("الحالة")}</th><th>${ar("المبلغ")}</th><th>${ar("الاستهلاك")}</th><th>${ar("العداد")}</th><th>${ar("المشترك")}</th></tr></thead>")
            appendLine("<tbody>")
            r.invoices.forEachIndexed { i, inv ->
                val cls = if (i % 2 == 0) "even" else "odd"
                val st = if (inv.status == "PAID") "مدفوعة" else "غير مدفوعة"
                val stCls = if (inv.status == "PAID") "green" else "red"
                appendLine("<tr class='$cls'><td class='$stCls'>${ar(st)}</td><td>${fmt(inv.totalAmount)} DH</td><td>${ar("${inv.consumption} م³")}</td><td>${e(inv.meterNumber ?: "-")}</td><td>${e(inv.subscriberName ?: "-")}</td></tr>")
            }
            appendLine("<tr class='amber-row'><td colspan='5'>${ar("المجموع الكلي: ${fmt(r.totalInvoicesAmount)} DH | ${r.totalInvoicesCount} فاتورة")}</td></tr>")
            appendLine("</tbody></table>")
        }

        // Transactions
        if (r.transactions.isNotEmpty()) {
            appendLine("<h2>${ar("المعاملات المالية")}</h2>")
            appendLine("<table>")
            appendLine("<thead><tr><th>${ar("التاريخ")}</th><th>${ar("المبلغ")}</th><th>${ar("الوصف")}</th><th>${ar("الصنف")}</th><th>${ar("النوع")}</th></tr></thead>")
            appendLine("<tbody>")
            r.transactions.forEachIndexed { i, x ->
                val cls = if (i % 2 == 0) "even" else "odd"
                val ty = if (x.type == "INCOME") "إيراد" else "مصروف"
                val tyCls = if (x.type == "EXPENSE") "red" else "green"
                appendLine("<tr class='$cls'><td>${fmtDate(x.date)}</td><td>${fmt(x.amount)} DH</td><td>${e(x.description ?: "-")}</td><td>${e(x.category)}</td><td class='$tyCls'>${ar(ty)}</td></tr>")
            }
            appendLine("</tbody></table>")
        }

        // Footer
        val now = java.time.LocalDateTime.now()
        val ts = String.format("تم الإنشاء: %02d/%02d/%d %02d:%02d", now.dayOfMonth, now.monthValue, now.year, now.hour, now.minute)
        appendLine("<div class='footer'>${ar(ts)}</div>")

        appendLine("</body></html>")
    }

    private fun e(s: String): String = esc(ar(s))
    private fun esc(s: String): String = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
    private fun ar(text: String): String {
        if (text.isEmpty()) return text
        val shaped = ArabicShaper.shape(text)
        val bidi = java.text.Bidi(shaped, java.text.Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT)
        if (bidi.isLeftToRight) return shaped
        val count = bidi.runCount
        val levels = ByteArray(count) { bidi.getRunLevel(it).toByte() }
        val order = Array<Any>(count) { it }
        java.text.Bidi.reorderVisually(levels, 0, order, 0, count)
        val sb = StringBuilder(shaped.length)
        for (v in 0 until count) {
            val ri = order[v] as Int
            val seg = shaped.substring(bidi.getRunStart(ri), bidi.getRunLimit(ri))
            sb.append(if (bidi.getRunLevel(ri) % 2 == 1) seg.reversed() else seg)
        }
        return sb.toString()
    }

    private fun fmt(v: Double): String = if (v == v.toLong().toDouble()) "${v.toLong()}" else String.format("%.2f", v)
    private fun fmtDate(epoch: Long): String {
        if (epoch <= 0) return "-"
        val d = Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).toLocalDate()
        return String.format("%02d/%02d/%d", d.dayOfMonth, d.monthValue, d.year)
    }
}
