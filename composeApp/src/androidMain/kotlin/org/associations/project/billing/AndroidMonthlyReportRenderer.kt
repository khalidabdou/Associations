package org.associations.project.billing

import com.itextpdf.html2pdf.ConverterProperties
import com.itextpdf.html2pdf.HtmlConverter
import com.itextpdf.layout.font.FontProvider
import org.associations.project.database.AppSettings
import org.associations.project.utils.ArabicShaper
import org.associations.project.reports.MonthlyReportData
import org.associations.project.database.GetAllInvoicesByMonth
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
        val paidInvoices = r.invoices.filter { it.status == "PAID" }
        val unpaidInvoices = r.invoices.filter { it.status != "PAID" }

        val monthlyFee = s.monthlyFixedFee
        val lateFee = s.lateFeeAmount
        var totalWaterCharge = 0.0
        var totalPenalty = 0.0
        var totalMonthlyFee = 0.0

        for (inv in r.invoices) {
            val penalty = if (inv.isPenaltyApplied == 1L) lateFee else 0.0
            val waterCharge = (inv.totalAmount - penalty - monthlyFee).coerceAtLeast(0.0)
            totalWaterCharge += waterCharge
            totalPenalty += penalty
            totalMonthlyFee += if (monthlyFee > 0.0) monthlyFee else 0.0
        }

        appendLine("<!DOCTYPE html>")
        appendLine("<html dir='rtl' lang='ar'>")
        appendLine("<head><meta charset='UTF-8'>")
        appendLine("<style>")
        appendLine("body { font-family: 'Amiri', 'Tajawal', sans-serif; direction: ltr; unicode-bidi: bidi-override; text-align: right; color: #333; margin: 24px; font-size: 10pt; }")
        appendLine("h1 { color: #19375f; font-size: 18pt; text-align: center; margin: 4px 0; }")
        appendLine("h2 { color: #19375f; font-size: 13pt; margin: 14px 0 8px; }")
        appendLine(".sub { color: #50505a; font-size: 10pt; margin: 2px 0; }")
        appendLine(".light { color: #9e9e9e; font-size: 9pt; text-align: center; margin: 2px 0; }")
        appendLine("hr { border: none; border-top: 1px solid #e0e0e0; margin: 8px 0; }")
        // --- Two-side header ---
        appendLine(".header-row { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 10px; }")
        appendLine(".header-left { width: 55%; text-align: right; }")
        appendLine(".header-right { width: 42%; }")
        appendLine(".header-right table { width: 100%; border-collapse: collapse; direction: ltr; font-size: 9pt; }")
        appendLine(".header-right td { padding: 3px 6px; text-align: right; border: none; }")
        appendLine(".header-right .label { color: #50505a; }")
        appendLine(".header-right .value { font-weight: bold; color: #19375f; }")
        appendLine(".header-right .red-value { font-weight: bold; color: #bf360c; }")
        appendLine(".header-right .green-value { font-weight: bold; color: #2e7d32; }")
        // --- Tables ---
        appendLine("table { width: 100%; border-collapse: collapse; direction: ltr; }")
        appendLine("th { background: #f5f7fa; color: #19375f; padding: 5px 6px; text-align: right; font-size: 9pt; border: none; }")
        appendLine("td { padding: 4px 6px; text-align: right; font-size: 9pt; border: none; }")
        appendLine("tr.even { background: #ffffff; }")
        appendLine("tr.odd { background: #f8f9fa; }")
        appendLine(".green { color: #2e7d32; }")
        appendLine(".red { color: #bf360c; }")
        appendLine(".primary { color: #19375f; }")
        appendLine(".amber-row td { background: #ffc107; font-weight: bold; padding: 6px; }")
        appendLine(".unpaid-header td { background: #fff3e0; color: #bf360c; font-weight: bold; padding: 5px 6px; font-size: 10pt; }")
        appendLine(".section-title { background: #e8eaf6; color: #19375f; padding: 5px 8px; font-weight: bold; font-size: 10pt; margin-top: 12px; margin-bottom: 4px; text-align: center; }")
        appendLine(".footer { font-size: 8pt; color: #999; text-align: center; margin-top: 16px; }")
        appendLine("</style>")
        appendLine("</head><body>")

        // ── TWO-SIDE HEADER ──
        appendLine("<div class='header-row'>")

        // Left side: logo + association info + dates
        appendLine("<div class='header-left'>")
        if (!s.logoPath.isNullOrBlank()) {
            try {
                val file = java.io.File(s.logoPath)
                if (file.exists()) {
                    val bytes = java.io.FileInputStream(file).use { it.readBytes() }
                    val b64 = java.util.Base64.getEncoder().encodeToString(bytes)
                    val ext = file.extension.lowercase()
                    val mime = if (ext == "png") "image/png" else if (ext == "jpg" || ext == "jpeg") "image/jpeg" else "image/png"
                    appendLine("<div style='text-align:center;margin-bottom:4px'><img src='data:$mime;base64,$b64' style='width:64px;height:64px'/></div>")
                }
            } catch (_: Exception) {}
        }
        appendLine("<h1>${e(s.associationName)}</h1>")
        if (s.associationAddress.isNotBlank()) appendLine("<div class='sub'>${e(s.associationAddress)}</div>")
        if (s.associationPhone.isNotBlank()) appendLine("<div class='sub'>${e(s.associationPhone)}</div>")
        appendLine("<hr/>")
        appendLine("<h1>${ar("التقرير الشهري - ${r.monthYear.displayName}")}</h1>")
        appendLine("<div class='light'>${ar("الفترة: ${fmtDate(r.monthYear.startEpochMillis)} – ${fmtDate(r.monthYear.endEpochMillis)}")}</div>")
        appendLine("</div>")

        // Right side: summary numbers
        appendLine("<div class='header-right'>")
        appendLine("<table>")
        appendLine("<tr class='even'><td class='label'>${ar("إجمالي الاستهلاك")}</td><td class='value'>${ar("${r.totalConsumption} م³")}</td></tr>")
        appendLine("<tr class='odd'><td class='label'>${ar("عدد الفواتير")}</td><td class='value'>${r.totalInvoicesCount}</td></tr>")
        appendLine("<tr class='even'><td class='label'>${ar("مبلغ الفواتير")}</td><td class='value'>${fmt(r.totalInvoicesAmount)} DH</td></tr>")
        appendLine("<tr class='odd'><td class='label'>${ar("فواتير مسددة")}</td><td class='green-value'>${r.paidInvoicesCount} | ${fmt(r.paidInvoicesAmount)} DH</td></tr>")
        appendLine("<tr class='even'><td class='label'>${ar("فواتير غير مسددة")}</td><td class='red-value'>${r.unpaidInvoicesCount} | ${fmt(r.unpaidInvoicesAmount)} DH</td></tr>")
        appendLine("<tr class='odd'><td class='label'>${ar("غرامات التأخير")}</td><td class='red-value'>${fmt(totalPenalty)} DH</td></tr>")
        appendLine("<tr class='even'><td class='label'>${ar("الإيرادات")}</td><td class='green-value'>+${fmt(r.totalIncomeAmount)} DH</td></tr>")
        appendLine("<tr class='odd'><td class='label'>${ar("المصاريف")}</td><td class='red-value'>-${fmt(r.totalExpensesAmount)} DH</td></tr>")
        appendLine("<tr class='even'><td class='label'>${ar("الرصيد الصافي")}</td><td class='value'>${fmt(r.netBalance)} DH</td></tr>")
        appendLine("</table>")
        appendLine("</div>")

        appendLine("</div>") // end header-row

        appendLine("<hr/>")

        // ── INVOICES TABLE ──
        if (r.invoices.isNotEmpty()) {
            appendLine("<h2>${ar("الفواتير")}</h2>")
            appendLine("<table>")
            appendLine("<thead><tr>")
            appendLine("<th>${ar("المجموع")}</th>")
            appendLine("<th>${ar("غرامة التأخير")}</th>")
            appendLine("<th>${ar("الرسوم الشهرية")}</th>")
            appendLine("<th>${ar("و. الاستهلاك")}</th>")
            appendLine("<th>${ar("الاستهلاك")}</th>")
            appendLine("<th>${ar("العداد")}</th>")
            appendLine("<th>${ar("المشترك")}</th>")
            appendLine("</tr></thead>")
            appendLine("<tbody>")

            // Paid invoices first
            if (paidInvoices.isNotEmpty()) {
                appendLine("<tr class='section-title'><td colspan='7'>${ar("✓ الفواتير المسددة - ${paidInvoices.size}")}</td></tr>")
                appendInvoiceRows(paidInvoices, monthlyFee, lateFee)
            }

            // Unpaid invoices at bottom
            if (unpaidInvoices.isNotEmpty()) {
                appendLine("<tr class='unpaid-header'><td colspan='7'>${ar("⚠ الفواتير غير المسددة - ${unpaidInvoices.size}")}</td></tr>")
                appendInvoiceRows(unpaidInvoices, monthlyFee, lateFee)
            }

            // Total row
            appendLine("<tr class='amber-row'>")
            appendLine("<td>${fmt(r.totalInvoicesAmount)} DH</td>")
            appendLine("<td>${fmt(totalPenalty)} DH</td>")
            appendLine("<td>${fmt(totalMonthlyFee)} DH</td>")
            appendLine("<td>${fmt(totalWaterCharge)} DH</td>")
            appendLine("<td>-</td><td>-</td>")
            appendLine("<td>${ar("المجموع الكلي - ${r.totalInvoicesCount} فاتورة")}</td>")
            appendLine("</tr>")

            appendLine("</tbody></table>")
        }

        // ── TRANSACTIONS ──
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

    private fun StringBuilder.appendInvoiceRows(
        invoices: List<GetAllInvoicesByMonth>,
        monthlyFee: Double,
        lateFee: Double
    ) {
        invoices.forEachIndexed { i, inv ->
            val cls = if (i % 2 == 0) "even" else "odd"
            val penalty = if (inv.isPenaltyApplied == 1L) lateFee else 0.0
            val waterCharge = (inv.totalAmount - penalty - monthlyFee).coerceAtLeast(0.0)
            val mf = if (monthlyFee > 0.0) monthlyFee else 0.0

            appendLine("<tr class='$cls'>")
            appendLine("<td>${fmt(inv.totalAmount)} DH</td>")
            appendLine("<td class='${if (penalty > 0.0) "red" else ""}'>${fmt(penalty)} DH</td>")
            appendLine("<td>${fmt(mf)} DH</td>")
            appendLine("<td>${fmt(waterCharge)} DH</td>")
            appendLine("<td>${ar("${inv.consumption} م³")}</td>")
            appendLine("<td>${e(inv.meterNumber ?: "-")}</td>")
            appendLine("<td>${e(inv.subscriberName ?: "-")}</td>")
            appendLine("</tr>")
        }
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
