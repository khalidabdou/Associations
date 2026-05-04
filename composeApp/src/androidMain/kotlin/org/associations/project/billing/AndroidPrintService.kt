package org.associations.project.billing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import org.associations.project.database.AppSettings
import org.associations.project.database.Invoice
import org.associations.project.database.Subscriber
import java.io.File
import java.io.FileOutputStream
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class AndroidPrintService(private val context: Context) : PrintService {
    override suspend fun printInvoice(
            invoice: Invoice,
            subscriber: Subscriber,
            settings: AppSettings
    ) {
        printInvoices(
            items = listOf(invoice to subscriber),
            settings = settings,
            jobName = "Invoice #${invoice.id} - ${subscriber.fullName}",
        )
    }

    override suspend fun printInvoices(
            items: List<Pair<Invoice, Subscriber>>,
            settings: AppSettings,
            jobName: String,
    ) {
        if (items.isEmpty()) return
        val bitmaps = items.map { (inv, sub) -> generateInvoiceImage(inv, sub, settings) }
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager

        val attributes = PrintAttributes.Builder().apply {
            if (settings.printFormat == "RECEIPT") {
                setMediaSize(PrintAttributes.MediaSize("ROLL_80MM", "80mm Roll", 3150, 11000))
                setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
            } else {
                setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            }
            setResolution(PrintAttributes.Resolution("default", "default", 300, 300))
        }.build()

        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback,
                    extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(jobName)
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(bitmaps.size)
                        .build()
                callback.onLayoutFinished(info, newAttributes == oldAttributes)
            }

            override fun onWrite(
                    pageRanges: Array<out PageRange>,
                    destination: ParcelFileDescriptor,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onWriteCancelled()
                    return
                }

                val pdfDocument = PdfDocument()
                bitmaps.forEachIndexed { index, bmp ->
                    val pageInfo = PdfDocument.PageInfo.Builder(bmp.width, bmp.height, index + 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    page.canvas.drawBitmap(bmp, 0f, 0f, null)
                    pdfDocument.finishPage(page)
                }

                try {
                    FileOutputStream(destination.fileDescriptor).use { output ->
                        pdfDocument.writeTo(output)
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                } finally {
                    pdfDocument.close()
                    destination.close()
                }
            }
        }

        printManager.print(jobName, adapter, attributes)
    }

    private fun generateInvoiceImage(
            invoice: Invoice,
            subscriber: Subscriber,
            settings: AppSettings
    ): Bitmap {
        val isReceipt = settings.printFormat == "RECEIPT"
        // Higher resolution for print quality
        val width = if (isReceipt) 640 else 1240
        val height = if (isReceipt) 1200 else 1754
        val margin = if (isReceipt) 32f else 90f
        val titleSize = if (isReceipt) 42f else 64f
        val bodySize = if (isReceipt) 28f else 36f
        val tableSize = if (isReceipt) 26f else 36f
        val totalSize = if (isReceipt) 38f else 52f
        val logoSize = if (isReceipt) 140f else 220f
        val lineGap = if (isReceipt) 34f else 44f

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint().apply { color = Color.WHITE }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val blackPaint = Paint().apply { color = Color.BLACK; isAntiAlias = true }
        val contentWidth = width - margin * 2
        var y = margin

        // Logo
        if (!settings.logoPath.isNullOrBlank()) {
            try {
                val logoFile = File(settings.logoPath)
                if (logoFile.exists()) {
                    val logoBitmap = android.graphics.BitmapFactory.decodeFile(settings.logoPath)
                    if (logoBitmap != null) {
                        val logoLeft = (width - logoSize) / 2f
                        val src = Rect(0, 0, logoBitmap.width, logoBitmap.height)
                        val dst = RectF(logoLeft, y, logoLeft + logoSize, y + logoSize)
                        canvas.drawBitmap(logoBitmap, src, dst, null)
                        y += logoSize + (if (isReceipt) 20f else 40f)
                    }
                }
            } catch (e: Exception) {
                println("Error loading logo for print: ${e.message}")
            }
        }

        // Header
        blackPaint.textSize = titleSize
        blackPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        drawCenteredText(canvas, blackPaint, settings.associationName, y, width)
        y += titleSize + 10f

        blackPaint.textSize = bodySize
        blackPaint.typeface = Typeface.DEFAULT
        if (settings.associationAddress.isNotBlank()) {
            drawCenteredText(canvas, blackPaint, settings.associationAddress, y, width)
            y += lineGap
        }
        if (settings.associationPhone.isNotBlank()) {
            drawCenteredText(canvas, blackPaint, settings.associationPhone, y, width)
            y += lineGap
        }
        y += 15f

        // Divider
        val linePaint = Paint().apply { color = Color.BLACK; strokeWidth = 2f }
        canvas.drawLine(margin, y, width - margin, y, linePaint)
        y += if (isReceipt) 30f else 50f

        // Title
        blackPaint.textSize = if (isReceipt) 34f else 48f
        blackPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        drawCenteredText(canvas, blackPaint, "فاتورة استهلاك الماء", y, width)
        y += if (isReceipt) 44f else 70f

        // Info
        val issueDate = Instant.fromEpochMilliseconds(invoice.issueDate)
                .toLocalDateTime(TimeZone.currentSystemDefault())

        blackPaint.textSize = bodySize
        blackPaint.typeface = Typeface.DEFAULT
        canvas.drawText("Invoice #: ${invoice.id}", margin, y, blackPaint)
        canvas.drawText("Date: ${issueDate.date}", margin, y + lineGap, blackPaint)
        drawRightAlignedText(canvas, blackPaint, subscriber.fullName, width - margin, y)
        val meterText = "Meter: ${subscriber.meterNumber}"
        canvas.drawText(meterText, width - margin - blackPaint.measureText(meterText), y + lineGap, blackPaint)
        y += lineGap * 2 + 12f

        // Table header
        blackPaint.textSize = tableSize
        blackPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val col1 = margin
        val col2 = margin + contentWidth / 3
        val col3 = margin + contentWidth * 2 / 3
        canvas.drawText("Current", col1, y, blackPaint)
        canvas.drawText("Previous", col2, y, blackPaint)
        canvas.drawText("Consumption", col3, y, blackPaint)
        y += 12f
        canvas.drawLine(margin, y, width - margin, y, linePaint)
        y += lineGap

        // Table row
        blackPaint.typeface = Typeface.DEFAULT
        canvas.drawText("${invoice.currentReading}", col1, y, blackPaint)
        canvas.drawText("${invoice.previousReading}", col2, y, blackPaint)
        canvas.drawText("${invoice.consumption} m\u00B3", col3, y, blackPaint)
        y += 18f
        canvas.drawLine(margin, y, width - margin, y, linePaint)
        y += if (isReceipt) 36f else 60f

        // Total
        blackPaint.textSize = totalSize
        blackPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("المجموع الكلي", margin, y, blackPaint)
        drawRightAlignedText(canvas, blackPaint, "${invoice.totalAmount} DH", width - margin, y)
        y += if (isReceipt) 60f else 90f

        // Due date notification
        if (invoice.dueDate > 0) {
            val dueDate = Instant.fromEpochMilliseconds(invoice.dueDate)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
            val boxPaint = Paint().apply { color = Color.rgb(255, 243, 224); style = Paint.Style.FILL }
            val borderPaint = Paint().apply { color = Color.rgb(230, 81, 0); style = Paint.Style.STROKE; strokeWidth = 3f }
            val boxHeight = if (isReceipt) 80f else 110f
            val rect = RectF(margin, y, width - margin, y + boxHeight)
            canvas.drawRoundRect(rect, 8f, 8f, boxPaint)
            canvas.drawRoundRect(rect, 8f, 8f, borderPaint)
            blackPaint.color = Color.rgb(191, 54, 12)
            blackPaint.textSize = bodySize
            blackPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            drawCenteredText(canvas, blackPaint, "اجل الدفع: ${dueDate.date}", y + boxHeight / 2 + bodySize / 3, width)
            blackPaint.color = Color.BLACK
            y += boxHeight + (if (isReceipt) 24f else 40f)
        }

        // Footer
        blackPaint.textSize = bodySize
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
}
