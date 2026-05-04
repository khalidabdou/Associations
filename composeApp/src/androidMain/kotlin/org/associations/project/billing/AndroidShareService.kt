package org.associations.project.billing

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import org.associations.project.database.AppSettings
import org.associations.project.database.Invoice
import org.associations.project.database.Subscriber
import java.io.File
import java.io.FileOutputStream
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class AndroidShareService(private val context: Context) : ShareService {
    override suspend fun shareInvoice(invoice: Invoice, subscriber: Subscriber, settings: AppSettings) {
        val bitmap = generateInvoiceImage(invoice, subscriber, settings)
        val uri = saveAndGetUri(bitmap, invoice.id)

        if (uri != null) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Invoice #${invoice.id} - ${settings.associationName}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "مشاركة الفاتورة")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }

    private fun generateInvoiceImage(invoice: Invoice, subscriber: Subscriber, settings: AppSettings): Bitmap {
        val width = 800
        val height = 1100
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // White background
        val bgPaint = Paint().apply { color = Color.WHITE }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val blackPaint = Paint().apply { color = Color.BLACK; isAntiAlias = true }
        val margin = 60f
        val contentWidth = width - margin * 2
        var y = margin

        // Draw logo if available
        if (!settings.logoPath.isNullOrBlank()) {
            try {
                val logoFile = File(settings.logoPath)
                if (logoFile.exists()) {
                    val logoBitmap = android.graphics.BitmapFactory.decodeFile(settings.logoPath)
                    if (logoBitmap != null) {
                        val logoSize = 160
                        val logoLeft = (width - logoSize) / 2f
                        val src = android.graphics.Rect(0, 0, logoBitmap.width, logoBitmap.height)
                        val dst = android.graphics.RectF(logoLeft, y, logoLeft + logoSize, y + logoSize)
                        canvas.drawBitmap(logoBitmap, src, dst, null)
                        y += logoSize + 30
                    }
                }
            } catch (e: Exception) {
                println("Error loading logo for share: ${e.message}")
            }
        }

        // Header - Association Name
        blackPaint.textSize = 48f
        blackPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        drawCenteredText(canvas, blackPaint, settings.associationName, y, width)
        y += 55

        blackPaint.textSize = 28f
        blackPaint.typeface = Typeface.DEFAULT
        if (settings.associationAddress.isNotBlank()) {
            drawCenteredText(canvas, blackPaint, settings.associationAddress, y, width)
            y += 35
        }
        if (settings.associationPhone.isNotBlank()) {
            drawCenteredText(canvas, blackPaint, settings.associationPhone, y, width)
            y += 35
        }
        y += 15

        // Divider
        val linePaint = Paint().apply { color = Color.BLACK; strokeWidth = 2f }
        canvas.drawLine(margin, y, width - margin, y, linePaint)
        y += 40

        // Title
        blackPaint.textSize = 36f
        blackPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        drawCenteredText(canvas, blackPaint, "فاتورة استهلاك الماء", y, width)
        y += 55

        // Invoice Info
        val date = Instant.fromEpochMilliseconds(invoice.issueDate)
            .toLocalDateTime(TimeZone.currentSystemDefault())

        blackPaint.textSize = 28f
        blackPaint.typeface = Typeface.DEFAULT
        canvas.drawText("Invoice #: ${invoice.id}", margin, y, blackPaint)
        canvas.drawText("Date: ${date.date}", margin, y + 35, blackPaint)

        // Right side - subscriber info
        drawRightAlignedText(canvas, blackPaint, subscriber.fullName, width - margin, y)
        canvas.drawText("Meter: ${subscriber.meterNumber}", width - margin - blackPaint.measureText("Meter: ${subscriber.meterNumber}"), y + 35, blackPaint)
        y += 80

        // Table header
        blackPaint.textSize = 28f
        blackPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val col1 = margin
        val col2 = margin + contentWidth / 3
        val col3 = margin + contentWidth * 2 / 3

        canvas.drawText("Current", col1, y, blackPaint)
        canvas.drawText("Previous", col2, y, blackPaint)
        canvas.drawText("Consumption", col3, y, blackPaint)
        y += 10
        canvas.drawLine(margin, y, width - margin, y, linePaint)
        y += 35

        // Table row
        blackPaint.typeface = Typeface.DEFAULT
        canvas.drawText("${invoice.currentReading}", col1, y, blackPaint)
        canvas.drawText("${invoice.previousReading}", col2, y, blackPaint)
        canvas.drawText("${invoice.consumption} m\u00B3", col3, y, blackPaint)
        y += 15
        canvas.drawLine(margin, y, width - margin, y, linePaint)
        y += 50

        // Total
        blackPaint.textSize = 40f
        blackPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("المجموع الكلي", margin, y, blackPaint)
        drawRightAlignedText(canvas, blackPaint, "${invoice.totalAmount} DH", width - margin, y)
        y += 80

        // Footer
        blackPaint.textSize = 28f
        blackPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        drawCenteredText(canvas, blackPaint, "شكرا لالتزامكم بتسديد واجباتكم", y, width)

        return bitmap
    }

    private fun drawCenteredText(canvas: Canvas, paint: Paint, text: String, y: Float, imgWidth: Int) {
        val x = (imgWidth - paint.measureText(text)) / 2f
        canvas.drawText(text, x, y, paint)
    }

    private fun drawRightAlignedText(canvas: Canvas, paint: Paint, text: String, rightX: Float, y: Float) {
        val x = rightX - paint.measureText(text)
        canvas.drawText(text, x, y, paint)
    }

    private fun saveAndGetUri(bitmap: Bitmap, invoiceId: Long): Uri? {
        return try {
            val imagesDir = File(context.cacheDir, "shared_invoices")
            if (!imagesDir.exists()) imagesDir.mkdirs()
            val imageFile = File(imagesDir, "invoice_$invoiceId.png")
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
        } catch (e: Exception) {
            println("Error saving invoice image: ${e.message}")
            null
        }
    }
}
