package org.associations.project.billing

import org.associations.project.database.Invoice
import org.associations.project.database.Subscriber
import org.associations.project.database.AppSettings
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.print.PageFormat
import java.awt.print.Printable
import java.awt.print.PrinterJob
import java.awt.Color
import java.awt.BasicStroke

class DesktopPrintService : PrintService {

    private fun formatAmount(amount: Double): String {
        return if (amount == amount.toLong().toDouble()) {
            "${amount.toLong()}"
        } else {
            String.format("%.2f", amount)
        }
    }

    override suspend fun printInvoice(invoice: Invoice, subscriber: Subscriber, settings: AppSettings) {
        printInvoices(
            items = listOf(invoice to subscriber),
            settings = settings,
            jobName = "Invoice ${invoice.id}",
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

        job.setPrintable(object : Printable {
            override fun print(graphics: Graphics, pageFormat: PageFormat, pageIndex: Int): Int {
                if (pageIndex >= items.size) return Printable.NO_SUCH_PAGE
                val (invoice, subscriber) = items[pageIndex]

                val g2d = graphics as Graphics2D
                g2d.translate(pageFormat.imageableX, pageFormat.imageableY)

                val width = pageFormat.imageableWidth.toInt()
                var y = 50
                val isPaid = invoice.status == "PAID"
                val penaltyApplied = invoice.isPenaltyApplied == 1L
                val penaltyValue = if (penaltyApplied) settings.lateFeeAmount else 0.0
                val monthlyFeeValue = if (settings.monthlyFixedFee > 0.0) settings.monthlyFixedFee else 0.0
                val waterChargeValue = (invoice.totalAmount - penaltyValue - monthlyFeeValue).coerceAtLeast(0.0)

                g2d.font = Font("Dialog", Font.BOLD, 18)
                g2d.color = Color.BLACK
                g2d.drawString(settings.associationName, 50, y)
                y += 25

                g2d.font = Font("Dialog", Font.PLAIN, 12)
                g2d.drawString(settings.associationAddress, 50, y)
                y += 15
                g2d.drawString(settings.associationPhone, 50, y)
                y += 30

                g2d.stroke = BasicStroke(1f)
                g2d.drawLine(50, y, width - 50, y)
                y += 30

                // Title (Arabic)
                g2d.font = Font("Dialog", Font.BOLD, 16)
                val title = if (isPaid) "وصل دفع فاتورة الماء" else "فاتورة استهلاك الماء"
                g2d.drawString(title, 50, y)
                y += 20

                // Paid stamp
                if (isPaid) {
                    g2d.color = Color(27, 94, 32)
                    g2d.font = Font("Dialog", Font.BOLD, 14)
                    g2d.drawString("✓ مدفوعة", 50, y)
                    g2d.color = Color.BLACK
                    y += 25
                }

                // Info (Arabic)
                g2d.font = Font("Dialog", Font.PLAIN, 12)
                val dateLabel = if (isPaid) "تاريخ الدفع" else "التاريخ"
                g2d.drawString("رقم الفاتورة: #${invoice.id}", 50, y)
                g2d.drawString("$dateLabel: ${java.time.Instant.ofEpochMilli(invoice.issueDate)}", 300, y)
                y += 20
                g2d.drawString("المشترك: ${subscriber.fullName}", 50, y)
                g2d.drawString("العداد: ${subscriber.meterNumber}", 300, y)
                y += 30

                // Table header (Arabic)
                g2d.font = Font("Dialog", Font.BOLD, 12)
                g2d.drawString("الحالية", 50, y)
                g2d.drawString("السابقة", 150, y)
                g2d.drawString("الاستهلاك", 250, y)
                y += 10
                g2d.drawLine(50, y, width - 50, y)
                y += 20

                // Table row
                g2d.font = Font("Dialog", Font.PLAIN, 12)
                g2d.drawString("${invoice.currentReading}", 50, y)
                g2d.drawString("${invoice.previousReading}", 150, y)
                g2d.drawString("${invoice.consumption} م³", 250, y)
                y += 30

                // Breakdown
                g2d.drawString("استهلاك الماء: ${formatAmount(waterChargeValue)} درهم", 50, y)
                y += 18
                if (monthlyFeeValue > 0.0) {
                    g2d.drawString("الرسوم الشهرية: ${formatAmount(monthlyFeeValue)} درهم", 50, y)
                    y += 18
                }
                if (penaltyValue > 0.0) {
                    g2d.color = Color(191, 54, 12)
                    g2d.drawString("غرامة التأخير: ${formatAmount(penaltyValue)} درهم", 50, y)
                    g2d.color = Color.BLACK
                    y += 18
                }
                y += 10

                // Total
                g2d.font = Font("Dialog", Font.BOLD, 16)
                if (isPaid) g2d.color = Color(27, 94, 32)
                g2d.drawString("المجموع الكلي: ${formatAmount(invoice.totalAmount)} درهم", 50, y)
                g2d.color = Color.BLACK
                y += 30

                // Status box
                if (isPaid) {
                    g2d.color = Color(27, 94, 32)
                    g2d.font = Font("Dialog", Font.BOLD, 13)
                    g2d.drawString("مدفوعة ✓", 50, y)
                    g2d.color = Color.BLACK
                }

                return Printable.PAGE_EXISTS
            }
        })

        if (job.printDialog()) {
            try {
                job.print()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
