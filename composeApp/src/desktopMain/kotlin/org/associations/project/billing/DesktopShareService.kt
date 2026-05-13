package org.associations.project.billing

import org.associations.project.database.AppSettings
import org.associations.project.database.Invoice
import org.associations.project.database.Subscriber
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Desktop
import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class DesktopShareService : ShareService {
    override suspend fun shareInvoice(invoice: Invoice, subscriber: Subscriber, settings: AppSettings) {
        val imgWidth = 800
        val imgHeight = 1100
        val image = BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()

        // White background
        g2d.color = Color.WHITE
        g2d.fillRect(0, 0, imgWidth, imgHeight)
        g2d.color = Color.BLACK

        val margin = 60
        val contentWidth = imgWidth - margin * 2
        var y = margin

        // Draw logo if available
        if (!settings.logoPath.isNullOrBlank()) {
            try {
                val logoFile = File(settings.logoPath)
                if (logoFile.exists()) {
                    val logoImage = ImageIO.read(logoFile)
                    if (logoImage != null) {
                        val logoSize = 80
                        g2d.drawImage(logoImage, (imgWidth - logoSize) / 2, y, logoSize, logoSize, null)
                        y += logoSize + 15
                    }
                }
            } catch (e: Exception) {
                println("Error loading logo for share: ${e.message}")
            }
        }

        // Header - Association Name (centered)
        g2d.font = Font("Dialog", Font.BOLD, 24)
        drawCenteredText(g2d, settings.associationName, y, imgWidth)
        y += 30

        g2d.font = Font("Dialog", Font.PLAIN, 14)
        if (settings.associationAddress.isNotBlank()) {
            drawCenteredText(g2d, settings.associationAddress, y, imgWidth)
            y += 20
        }
        if (settings.associationPhone.isNotBlank()) {
            drawCenteredText(g2d, settings.associationPhone, y, imgWidth)
            y += 20
        }
        y += 10

        // Divider line
        g2d.stroke = BasicStroke(1f)
        g2d.drawLine(margin, y, imgWidth - margin, y)
        y += 25

        // Title
        g2d.font = Font("Dialog", Font.BOLD, 18)
        drawCenteredText(g2d, "فاتورة استهلاك الماء", y, imgWidth)
        y += 35

        // Invoice Info - two columns
        val date = Instant.fromEpochMilliseconds(invoice.issueDate)
            .toLocalDateTime(TimeZone.currentSystemDefault())

        g2d.font = Font("Dialog", Font.PLAIN, 14)
        g2d.drawString("رقم الفاتورة: ${invoice.id}", margin, y)
        g2d.drawString("التاريخ: ${date.date}", margin, y + 20)
        y += 10

        val rightX = imgWidth - margin
        drawRightAlignedText(g2d, subscriber.fullName, rightX, y)
        g2d.font = Font("Dialog", Font.PLAIN, 14)
        drawRightAlignedText(g2d, "العداد: ${subscriber.meterNumber}", rightX, y + 20)
        y += 45

        // Table header
        g2d.font = Font("Dialog", Font.BOLD, 14)
        val col1 = margin
        val col2 = margin + contentWidth / 3
        val col3 = margin + contentWidth * 2 / 3

        g2d.drawString("الحالية", col1, y)
        g2d.drawString("السابقة", col2, y)
        g2d.drawString("الاستهلاك", col3, y)
        y += 5
        g2d.drawLine(margin, y, imgWidth - margin, y)
        y += 20

        // Table row
        g2d.font = Font("Dialog", Font.PLAIN, 14)
        g2d.drawString("${invoice.currentReading}", col1, y)
        g2d.drawString("${invoice.previousReading}", col2, y)
        g2d.drawString("${invoice.consumption} م³", col3, y)
        y += 10
        g2d.drawLine(margin, y, imgWidth - margin, y)
        y += 30

        // Total
        g2d.font = Font("Dialog", Font.BOLD, 20)
        g2d.drawString("المجموع الكلي", margin, y)
        drawRightAlignedText(g2d, "${invoice.totalAmount} درهم", imgWidth - margin, y)
        y += 50

        // --- Payment Deadline (notification highlight) ---
        if (invoice.dueDate > 0) {
            val dueDate = Instant.fromEpochMilliseconds(invoice.dueDate)
                .toLocalDateTime(TimeZone.currentSystemDefault())
            g2d.color = Color(255, 243, 224)
            g2d.fillRect(margin, y, imgWidth - margin * 2, 40)
            g2d.color = Color(230, 81, 0)
            g2d.drawRect(margin, y, imgWidth - margin * 2, 40)
            g2d.font = Font("Dialog", Font.BOLD, 14)
            g2d.color = Color(191, 54, 12)
            drawCenteredText(g2d, "اجل الدفع: ${dueDate.date}", y + 28, imgWidth)
            g2d.color = Color.BLACK
            y += 55
        }

        // Footer
        g2d.font = Font("Dialog", Font.ITALIC, 14)
        drawCenteredText(g2d, "شكرا لالتزامكم بتسديد واجباتكم", y, imgWidth)

        g2d.dispose()

        // Save PNG to temp file
        val tempDir = System.getProperty("java.io.tmpdir")
        val pngFile = File(tempDir, "invoice_${invoice.id}.png")
        ImageIO.write(image, "png", pngFile)

        // Open with system viewer (allows sharing/saving)
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(pngFile)
        }
    }

    private fun drawCenteredText(g2d: Graphics2D, text: String, y: Int, imgWidth: Int) {
        val metrics = g2d.fontMetrics
        val x = (imgWidth - metrics.stringWidth(text)) / 2
        g2d.drawString(text, x, y)
    }

    private fun drawRightAlignedText(g2d: Graphics2D, text: String, rightX: Int, y: Int) {
        val metrics = g2d.fontMetrics
        val x = rightX - metrics.stringWidth(text)
        g2d.drawString(text, x, y)
    }
}
