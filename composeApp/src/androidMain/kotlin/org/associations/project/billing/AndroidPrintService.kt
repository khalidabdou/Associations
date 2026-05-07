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
        val isPaid = invoice.status == "PAID"
        val penaltyApplied = invoice.isPenaltyApplied == 1L
        val penaltyValue = if (penaltyApplied) settings.lateFeeAmount else 0.0
        val monthlyFeeValue = if (settings.monthlyFixedFee > 0.0) settings.monthlyFixedFee else 0.0
        val waterChargeValue = (invoice.totalAmount - penaltyValue - monthlyFeeValue).coerceAtLeast(0.0)

        blackPaint.textSize = if (isReceipt) 34f else 48f
        blackPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        drawCenteredText(
            canvas, blackPaint,
            if (isPaid) "وصل دفع فاتورة الماء" else "فاتورة استهلاك الماء",
            y, width
        )
        y += if (isReceipt) 44f else 70f

        // Paid stamp under title
        if (isPaid) {
            val stampPaint = Paint().apply {
                color = Color.rgb(46, 125, 50); style = Paint.Style.STROKE; strokeWidth = 3f
                isAntiAlias = true
            }
            blackPaint.color = Color.rgb(27, 94, 32)
            blackPaint.textSize = bodySize
            blackPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val stampText = "✓ مدفوعة"
            val tw = blackPaint.measureText(stampText)
            val pad = if (isReceipt) 12f else 20f
            val boxLeft = (width - tw) / 2f - pad
            val boxRight = (width + tw) / 2f + pad
            val boxBottom = y + bodySize + 8f
            canvas.drawRoundRect(RectF(boxLeft, y - bodySize, boxRight, boxBottom), 6f, 6f, stampPaint)
            drawCenteredText(canvas, blackPaint, stampText, y, width)
            blackPaint.color = Color.BLACK
            y += boxBottom - (y - bodySize) + 6f
        }

        // Info
        val issueDate = Instant.fromEpochMilliseconds(invoice.issueDate)
                .toLocalDateTime(TimeZone.currentSystemDefault())

        blackPaint.textSize = bodySize
        blackPaint.typeface = Typeface.DEFAULT
        // Right-aligned Arabic labels (label: value)
        drawRightAlignedText(canvas, blackPaint, "رقم الفاتورة: #${invoice.id}", width - margin, y)
        canvas.drawText(subscriber.fullName, margin, y, blackPaint)
        y += lineGap
        val dateLabel = if (isPaid) "تاريخ الدفع" else "التاريخ"
        drawRightAlignedText(canvas, blackPaint, "$dateLabel: ${issueDate.date}", width - margin, y)
        canvas.drawText("العداد: ${subscriber.meterNumber}", margin, y, blackPaint)
        y += lineGap + 12f

        // Table header (Arabic, RTL ordering)
        blackPaint.textSize = tableSize
        blackPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val col1 = margin
        val col2 = margin + contentWidth / 3
        val col3 = margin + contentWidth * 2 / 3
        canvas.drawText("الحالية", col1, y, blackPaint)
        canvas.drawText("السابقة", col2, y, blackPaint)
        canvas.drawText("الاستهلاك", col3, y, blackPaint)
        y += 12f
        canvas.drawLine(margin, y, width - margin, y, linePaint)
        y += lineGap

        // Table row
        blackPaint.typeface = Typeface.DEFAULT
        canvas.drawText("${invoice.currentReading}", col1, y, blackPaint)
        canvas.drawText("${invoice.previousReading}", col2, y, blackPaint)
        canvas.drawText("${invoice.consumption} م³", col3, y, blackPaint)
        y += 18f
        canvas.drawLine(margin, y, width - margin, y, linePaint)
        y += if (isReceipt) 24f else 40f

        // Breakdown (charges + monthly fee + penalty)
        blackPaint.textSize = bodySize
        blackPaint.typeface = Typeface.DEFAULT
        drawRightAlignedText(canvas, blackPaint, "استهلاك الماء", width - margin, y)
        canvas.drawText("${formatAmount(waterChargeValue)} درهم", margin, y, blackPaint)
        y += lineGap
        if (monthlyFeeValue > 0.0) {
            drawRightAlignedText(canvas, blackPaint, "الرسوم الشهرية", width - margin, y)
            canvas.drawText("${formatAmount(monthlyFeeValue)} درهم", margin, y, blackPaint)
            y += lineGap
        }
        if (penaltyValue > 0.0) {
            blackPaint.color = Color.rgb(191, 54, 12)
            drawRightAlignedText(canvas, blackPaint, "غرامة التأخير", width - margin, y)
            canvas.drawText("${formatAmount(penaltyValue)} درهم", margin, y, blackPaint)
            blackPaint.color = Color.BLACK
            y += lineGap
        }
        y += if (isReceipt) 8f else 16f

        // Total
        blackPaint.textSize = totalSize
        blackPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        if (isPaid) blackPaint.color = Color.rgb(27, 94, 32)
        drawRightAlignedText(canvas, blackPaint, "المجموع الكلي", width - margin, y)
        canvas.drawText("${formatAmount(invoice.totalAmount)} درهم", margin, y, blackPaint)
        blackPaint.color = Color.BLACK
        y += if (isReceipt) 50f else 80f

        // Status box
        if (isPaid) {
            val boxPaint = Paint().apply { color = Color.rgb(232, 245, 233); style = Paint.Style.FILL }
            val borderPaint = Paint().apply { color = Color.rgb(46, 125, 50); style = Paint.Style.STROKE; strokeWidth = 3f }
            val boxHeight = if (isReceipt) 80f else 110f
            val rect = RectF(margin, y, width - margin, y + boxHeight)
            canvas.drawRoundRect(rect, 8f, 8f, boxPaint)
            canvas.drawRoundRect(rect, 8f, 8f, borderPaint)
            blackPaint.color = Color.rgb(27, 94, 32)
            blackPaint.textSize = bodySize
            blackPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            drawCenteredText(
                canvas, blackPaint,
                "مدفوعة ✓ تاريخ الدفع: ${issueDate.date}",
                y + boxHeight / 2 + bodySize / 3, width
            )
            blackPaint.color = Color.BLACK
            y += boxHeight + (if (isReceipt) 24f else 40f)
        } else if (invoice.dueDate > 0) {
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

    private fun formatAmount(value: Double): String {
        val rounded = (value * 100).toLong() / 100.0
        return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
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
