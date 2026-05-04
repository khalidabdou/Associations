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

                g2d.font = Font("Dialog", Font.BOLD, 16)
                g2d.drawString("INVOICE / فاتورة", 50, y)
                y += 30

                g2d.font = Font("Dialog", Font.PLAIN, 12)
                g2d.drawString("Invoice #: ${invoice.id}", 50, y)
                g2d.drawString("Date: ${java.time.Instant.ofEpochMilli(invoice.issueDate)}", 300, y)
                y += 20
                g2d.drawString("Subscriber: ${subscriber.fullName}", 50, y)
                g2d.drawString("Meter: ${subscriber.meterNumber}", 300, y)
                y += 40

                g2d.font = Font("Dialog", Font.BOLD, 12)
                g2d.drawString("Previous", 50, y)
                g2d.drawString("Current", 150, y)
                g2d.drawString("Consumption", 250, y)
                g2d.drawString("Amount", 400, y)
                y += 10
                g2d.drawLine(50, y, width - 50, y)
                y += 20

                g2d.font = Font("Dialog", Font.PLAIN, 12)
                g2d.drawString("${invoice.previousReading}", 50, y)
                g2d.drawString("${invoice.currentReading}", 150, y)
                g2d.drawString("${invoice.consumption}", 250, y)
                g2d.drawString("${invoice.totalAmount} DH", 400, y)
                y += 40

                g2d.font = Font("Dialog", Font.BOLD, 16)
                g2d.drawString("Total: ${invoice.totalAmount} DH", 350, y)

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
