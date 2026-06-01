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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.associations.project.database.AppSettings
import org.associations.project.database.Invoice
import org.associations.project.database.Subscriber
import org.associations.project.reports.MonthlyReportData
import java.io.ByteArrayOutputStream
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
            jobName = "فاتورة ${invoice.id} - ${subscriber.fullName}",
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
            when (settings.printFormat) {
                "RECEIPT" -> {
                    setMediaSize(PrintAttributes.MediaSize("ROLL_80MM", "80mm Roll", 3150, 11000))
                    setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
                }
                "A5" -> setMediaSize(PrintAttributes.MediaSize.ISO_A5)
                else -> setMediaSize(PrintAttributes.MediaSize.ISO_A4)
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

    override suspend fun printNotification(
            invoice: Invoice,
            subscriber: Subscriber,
            settings: AppSettings
    ) {
        val bitmap = generateNotificationImage(invoice, subscriber, settings)
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager

        val attributes = PrintAttributes.Builder().apply {
            when (settings.printFormat) {
                "RECEIPT" -> {
                    setMediaSize(PrintAttributes.MediaSize("ROLL_80MM", "80mm Roll", 3150, 11000))
                    setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
                }
                "A5" -> setMediaSize(PrintAttributes.MediaSize.ISO_A5)
                else -> setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            }
            setResolution(PrintAttributes.Resolution("default", "default", 300, 300))
        }.build()

        val jobName = "إشعار دفع - ${subscriber.fullName}"
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
                        .setPageCount(1)
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
                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                pdfDocument.finishPage(page)
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

    override suspend fun printMonthlyReport(
            report: MonthlyReportData,
            settings: AppSettings
    ) {
        val pages = withContext(Dispatchers.IO) { generateMonthlyReportPages(report, settings) }
        val pdfBytes = withContext(Dispatchers.IO) { bitmapsToPdfBytes(pages) }
        printPdfBytes(pdfBytes, "التقرير الشهري - ${report.monthYear.displayName}")
    }

    override suspend fun exportMonthlyReport(
            report: MonthlyReportData,
            settings: AppSettings,
            outputStream: java.io.OutputStream
    ) {
        val pages = withContext(Dispatchers.IO) { generateMonthlyReportPages(report, settings) }
        withContext(Dispatchers.IO) {
            val pdfDocument = PdfDocument()
            try {
                pages.forEachIndexed { index, bmp ->
                    val pageInfo = PdfDocument.PageInfo.Builder(bmp.width, bmp.height, index + 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    page.canvas.drawBitmap(bmp, 0f, 0f, null)
                    pdfDocument.finishPage(page)
                }
                pdfDocument.writeTo(outputStream)
                outputStream.flush()
            } finally {
                pdfDocument.close()
            }
        }
    }

    private fun bitmapsToPdfBytes(pages: List<Bitmap>): ByteArray {
        val pdfDocument = PdfDocument()
        return try {
            pages.forEachIndexed { index, bmp ->
                val pageInfo = PdfDocument.PageInfo.Builder(bmp.width, bmp.height, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                page.canvas.drawBitmap(bmp, 0f, 0f, null)
                pdfDocument.finishPage(page)
            }
            val baos = ByteArrayOutputStream()
            pdfDocument.writeTo(baos)
            baos.toByteArray()
        } finally {
            pdfDocument.close()
        }
    }

    private fun printPdfBytes(pdfBytes: ByteArray, jobName: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val attributes = PrintAttributes.Builder().apply {
            setMediaSize(PrintAttributes.MediaSize.ISO_A4)
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
                        .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                        .build()
                callback.onLayoutFinished(info, newAttributes != oldAttributes)
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
                try {
                    FileOutputStream(destination.fileDescriptor).use { out ->
                        out.write(pdfBytes)
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                } finally {
                    destination.close()
                }
            }
        }

        printManager.print(jobName, adapter, attributes)
    }

    private fun generateMonthlyReportPages(
            report: MonthlyReportData,
            settings: AppSettings
    ): List<Bitmap> {
        // A4 dimensions only
        val width = 1240
        val height = 1754
        val margin = 90f
        val leftMargin = margin
        val rightMargin = width - margin
        val contentWidth = rightMargin - leftMargin

        val titleSize = 52f
        val subtitleSize = 40f
        val bodySize = 32f
        val tableSize = 28f
        val totalSize = 44f
        val lineGap = 40f

        val pages = mutableListOf<Bitmap>()
        val blackPaint = Paint().apply { color = Color.BLACK; isAntiAlias = true }
        val linePaint = Paint().apply { color = Color.BLACK; strokeWidth = 2f }

        // ── Helper to create a new page ──
        fun newPage(): Pair<Bitmap, Canvas> {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val bgPaint = Paint().apply { color = Color.WHITE }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
            return bitmap to canvas
        }

        // ── Helper: draw header on a canvas, returns y after header ──
        fun drawHeader(canvas: Canvas, yStart: Float): Float {
            var y = yStart

            // Logo
            if (!settings.logoPath.isNullOrBlank()) {
                try {
                    val logoFile = File(settings.logoPath)
                    if (logoFile.exists()) {
                        val logoBitmap = android.graphics.BitmapFactory.decodeFile(settings.logoPath)
                        if (logoBitmap != null) {
                            val logoSize = 160f
                            val logoLeft = (width - logoSize) / 2f
                            val src = Rect(0, 0, logoBitmap.width, logoBitmap.height)
                            val dst = RectF(logoLeft, y, logoLeft + logoSize, y + logoSize)
                            canvas.drawBitmap(logoBitmap, src, dst, null)
                            y += logoSize + 30f
                        }
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }

            blackPaint.textSize = totalSize
            blackPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            drawCenteredText(canvas, blackPaint, settings.associationName, y, width)
            y += totalSize + 8f

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
            y += 12f
            canvas.drawLine(margin, y, rightMargin, y, linePaint)
            y += 30f

            return y
        }

        // ── Helper: draw page footer ──
        fun drawFooter(canvas: Canvas, pageNum: Int, totalPages: Int) {
            val footerY = height - 50f
            canvas.drawLine(margin, footerY - 10f, rightMargin, footerY - 10f, linePaint)
            blackPaint.textSize = 24f
            blackPaint.typeface = Typeface.DEFAULT
            val footerText = "صفحة $pageNum / $totalPages"
            drawCenteredText(canvas, blackPaint, footerText, footerY + 20f, width)
        }

        // ── Build page 1: Header + Summary + Financial + first invoices ──
        val (bmp1, canvas1) = newPage()
        pages.add(bmp1)
        var y = drawHeader(canvas1, margin)

        // Report title
        blackPaint.textSize = titleSize
        blackPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        drawCenteredText(canvas1, blackPaint, "التقرير الشهري - ${report.monthYear.displayName}", y, width)
        y += titleSize + 24f
        canvas1.drawLine(margin, y, rightMargin, y, linePaint)
        y += 30f

        // ── Summary statistics box ──
        val boxPaint = Paint().apply { color = Color.rgb(245, 245, 255); style = Paint.Style.FILL }
        val boxBorder = Paint().apply { color = Color.rgb(100, 100, 180); style = Paint.Style.STROKE; strokeWidth = 2f }

        val summaryLines = listOf(
            "إجمالي الاستهلاك: ${report.totalConsumption} م³",
            "عدد الفواتير: ${report.totalInvoicesCount}  |  المبلغ: ${formatAmount(report.totalInvoicesAmount)} درهم",
            "الفواتير المسددة: ${report.paidInvoicesCount}  |  المبلغ: ${formatAmount(report.paidInvoicesAmount)} درهم",
            "الفواتير الغير مسددة: ${report.unpaidInvoicesCount}  |  المبلغ: ${formatAmount(report.unpaidInvoicesAmount)} درهم",
        )

        val summaryBoxHeight = summaryLines.size * lineGap + 40f
        val summaryRect = RectF(margin, y - 10f, rightMargin, y + summaryBoxHeight)
        canvas1.drawRoundRect(summaryRect, 10f, 10f, boxPaint)
        canvas1.drawRoundRect(summaryRect, 10f, 10f, boxBorder)

        y += lineGap
        blackPaint.textSize = bodySize
        blackPaint.typeface = Typeface.DEFAULT
        for (line in summaryLines) {
            drawRightAlignedText(canvas1, blackPaint, line, rightMargin - 20f, y)
            y += lineGap
        }
        y += 16f

        // ── Financial summary ──
        blackPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        blackPaint.textSize = subtitleSize
        drawCenteredText(canvas1, blackPaint, "الملخص المالي", y, width)
        y += subtitleSize + 12f

        val finBoxPaint = Paint().apply { color = Color.rgb(232, 245, 233); style = Paint.Style.FILL }
        val finBorder = Paint().apply { color = Color.rgb(46, 125, 50); style = Paint.Style.STROKE; strokeWidth = 2f }
        val finLines = listOf(
            "الإيرادات: +${formatAmount(report.totalIncomeAmount)} درهم",
            "المصاريف: -${formatAmount(report.totalExpensesAmount)} درهم",
            "الرصيد الصافي: ${formatAmount(report.netBalance)} درهم",
        )
        val finBoxHeight = finLines.size * lineGap + 30f
        val finRect = RectF(margin + 40f, y, rightMargin - 40f, y + finBoxHeight)
        canvas1.drawRoundRect(finRect, 8f, 8f, finBoxPaint)
        canvas1.drawRoundRect(finRect, 8f, 8f, finBorder)

        y += lineGap
        blackPaint.textSize = bodySize
        blackPaint.typeface = Typeface.DEFAULT
        for (line in finLines) {
            drawRightAlignedText(canvas1, blackPaint, line, rightMargin - 60f, y)
            y += lineGap
        }
        y += 20f

        // ── Invoices table section ──
        blackPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        blackPaint.textSize = subtitleSize
        drawRightAlignedText(canvas1, blackPaint, "الفواتير", rightMargin, y)
        y += lineGap
        canvas1.drawLine(margin, y, rightMargin, y, linePaint)
        y += 10f

        // Table header
        blackPaint.textSize = tableSize
        blackPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val colSub = leftMargin
        val colMeter = leftMargin + contentWidth * 0.22f
        val colCons = leftMargin + contentWidth * 0.42f
        val colAmt = leftMargin + contentWidth * 0.60f
        val colStat = leftMargin + contentWidth * 0.78f
        canvas1.drawText("المشترك", colSub, y, blackPaint)
        canvas1.drawText("العداد", colMeter, y, blackPaint)
        canvas1.drawText("الاستهلاك", colCons, y, blackPaint)
        canvas1.drawText("المبلغ", colAmt, y, blackPaint)
        canvas1.drawText("الحالة", colStat, y, blackPaint)
        y += 10f
        canvas1.drawLine(margin, y, rightMargin, y, linePaint)
        y += lineGap

        // Table rows — fit as many as possible on remaining space
        blackPaint.textSize = tableSize
        blackPaint.typeface = Typeface.DEFAULT
        var invoiceIndex = 0
        val remainingHeight = height - y - 80f  // 80f for footer
        val maxRowsPage1 = (remainingHeight / lineGap).toInt().coerceAtMost(report.invoices.size)

        for (i in 0 until maxRowsPage1) {
            val inv = report.invoices[i]
            canvas1.drawText(inv.subscriberName ?: "", colSub, y, blackPaint)
            canvas1.drawText(inv.meterNumber ?: "", colMeter, y, blackPaint)
            canvas1.drawText("${inv.consumption} م³", colCons, y, blackPaint)
            canvas1.drawText("${formatAmount(inv.totalAmount)}", colAmt, y, blackPaint)
            val statusText = if (inv.status == "PAID") "مدفوعة" else "غير مدفوعة"
            if (inv.status != "PAID") {
                blackPaint.color = Color.rgb(191, 54, 12)
            }
            canvas1.drawText(statusText, colStat, y, blackPaint)
            blackPaint.color = Color.BLACK
            y += lineGap
            invoiceIndex = i + 1
        }

        drawFooter(canvas1, 1, -1) // total pages unknown yet, will be fixed later

        // ── Subsequent pages for remaining invoices + transactions ──
        val itemsPerPage = ((height - margin - 120f) / lineGap).toInt()
        var pageNum = 2

        while (invoiceIndex < report.invoices.size) {
            val (bmp, canvas) = newPage()
            pages.add(bmp)
            y = margin

            // Page header
            blackPaint.textSize = subtitleSize
            blackPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            drawRightAlignedText(canvas, blackPaint, "الفواتير (تابع)", rightMargin, y)
            y += lineGap
            canvas.drawLine(margin, y, rightMargin, y, linePaint)
            y += 10f

            // Table header
            blackPaint.textSize = tableSize
            canvas.drawText("المشترك", colSub, y, blackPaint)
            canvas.drawText("العداد", colMeter, y, blackPaint)
            canvas.drawText("الاستهلاك", colCons, y, blackPaint)
            canvas.drawText("المبلغ", colAmt, y, blackPaint)
            canvas.drawText("الحالة", colStat, y, blackPaint)
            y += 10f
            canvas.drawLine(margin, y, rightMargin, y, linePaint)
            y += lineGap

            blackPaint.textSize = tableSize
            blackPaint.typeface = Typeface.DEFAULT

            val rowsThisPage = minOf(itemsPerPage, report.invoices.size - invoiceIndex)
            for (i in 0 until rowsThisPage) {
                val inv = report.invoices[invoiceIndex + i]
                canvas.drawText(inv.subscriberName ?: "", colSub, y, blackPaint)
                canvas.drawText(inv.meterNumber ?: "", colMeter, y, blackPaint)
                canvas.drawText("${inv.consumption} م³", colCons, y, blackPaint)
                canvas.drawText("${formatAmount(inv.totalAmount)}", colAmt, y, blackPaint)
                val statusText = if (inv.status == "PAID") "مدفوعة" else "غير مدفوعة"
                if (inv.status != "PAID") {
                    blackPaint.color = Color.rgb(191, 54, 12)
                }
                canvas.drawText(statusText, colStat, y, blackPaint)
                blackPaint.color = Color.BLACK
                y += lineGap
            }
            invoiceIndex += rowsThisPage

            // If no more invoices, start transactions
            if (invoiceIndex >= report.invoices.size && report.transactions.isNotEmpty()) {
                y += 20f
                blackPaint.textSize = subtitleSize
                blackPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                drawRightAlignedText(canvas, blackPaint, "المعاملات المالية", rightMargin, y)
                y += lineGap
                canvas.drawLine(margin, y, rightMargin, y, linePaint)
                y += 10f

                // Transaction table header
                blackPaint.textSize = tableSize
                val colTType = leftMargin
                val colTCat = leftMargin + contentWidth * 0.12f
                val colTAmt = leftMargin + contentWidth * 0.38f
                val colTDesc = leftMargin + contentWidth * 0.58f
                canvas.drawText("النوع", colTType, y, blackPaint)
                canvas.drawText("الصنف", colTCat, y, blackPaint)
                canvas.drawText("المبلغ", colTAmt, y, blackPaint)
                canvas.drawText("الوصف", colTDesc, y, blackPaint)
                y += 10f
                canvas.drawLine(margin, y, rightMargin, y, linePaint)
                y += lineGap

                blackPaint.textSize = tableSize
                blackPaint.typeface = Typeface.DEFAULT
                for (txn in report.transactions) {
                    if (y > height - 100f) break // don't overflow
                    val typeText = if (txn.type == "INCOME") "إيراد" else "مصروف"
                    if (txn.type == "EXPENSE") {
                        blackPaint.color = Color.rgb(191, 54, 12)
                    } else {
                        blackPaint.color = Color.rgb(27, 94, 32)
                    }
                    canvas.drawText(typeText, colTType, y, blackPaint)
                    blackPaint.color = Color.BLACK
                    canvas.drawText(txn.category, colTCat, y, blackPaint)
                    canvas.drawText("${formatAmount(txn.amount)}", colTAmt, y, blackPaint)
                    canvas.drawText(txn.description ?: "", colTDesc, y, blackPaint)
                    y += lineGap
                }
            }

            drawFooter(canvas, pageNum, -1)
            pageNum++
        }

        // ── Fix page numbers ──
        val totalPages = pages.size
        for ((idx, bmp) in pages.withIndex()) {
            val canvas = Canvas(bmp)
            val footerY = height - 50f
            // Redraw footer area
            val footerBg = Paint().apply { color = Color.WHITE }
            canvas.drawRect(0f, footerY - 20f, width.toFloat(), height.toFloat(), footerBg)
            canvas.drawLine(margin, footerY - 10f, rightMargin, footerY - 10f, linePaint)
            blackPaint.textSize = 24f
            blackPaint.typeface = Typeface.DEFAULT
            val footerText = "صفحة ${idx + 1} / $totalPages"
            drawCenteredText(canvas, blackPaint, footerText, footerY + 20f, width)
        }

        return pages
    }

    private fun generateInvoiceImage(
            invoice: Invoice,
            subscriber: Subscriber,
            settings: AppSettings
    ): Bitmap {
        val isReceipt = settings.printFormat == "RECEIPT"
        val isA5 = settings.printFormat == "A5"
        // Higher resolution for print quality
        val width = when {
            isReceipt -> 640
            isA5 -> 874
            else -> 1240
        }
        val height = when {
            isReceipt -> 1200
            isA5 -> 1240
            else -> 1754
        }
        val margin = when {
            isReceipt -> 32f
            isA5 -> 60f
            else -> 90f
        }
        val titleSize = when {
            isReceipt -> 42f
            isA5 -> 50f
            else -> 64f
        }
        val bodySize = when {
            isReceipt -> 28f
            isA5 -> 32f
            else -> 36f
        }
        val tableSize = when {
            isReceipt -> 26f
            isA5 -> 30f
            else -> 36f
        }
        val totalSize = when {
            isReceipt -> 38f
            isA5 -> 44f
            else -> 52f
        }
        val logoSize = when {
            isReceipt -> 140f
            isA5 -> 180f
            else -> 220f
        }
        val lineGap = when {
            isReceipt -> 34f
            isA5 -> 40f
            else -> 44f
        }

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
        drawRightAlignedText(canvas, blackPaint, "رقم الفاتورة: ${invoice.id}", width - margin, y)
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

        return bitmap
    }

    private fun generateNotificationImage(
            invoice: Invoice,
            subscriber: Subscriber,
            settings: AppSettings
    ): Bitmap {
        val isReceipt = settings.printFormat == "RECEIPT"
        val isA5 = settings.printFormat == "A5"
        val width = when {
            isReceipt -> 640
            isA5 -> 874
            else -> 1240
        }
        val height = when {
            isReceipt -> 900
            isA5 -> 1100
            else -> 1400
        }
        val margin = when {
            isReceipt -> 32f
            isA5 -> 60f
            else -> 90f
        }
        val titleSize = when {
            isReceipt -> 32f
            isA5 -> 40f
            else -> 52f
        }
        val bodySize = when {
            isReceipt -> 22f
            isA5 -> 26f
            else -> 32f
        }
        val lineGap = when {
            isReceipt -> 28f
            isA5 -> 34f
            else -> 44f
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bgPaint = Paint().apply { color = Color.WHITE }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val blackPaint = Paint().apply { color = Color.BLACK; isAntiAlias = true }
        var y = margin + 20f

        // Header - Association Name
        blackPaint.textSize = titleSize
        blackPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        drawCenteredText(canvas, blackPaint, settings.associationName, y, width)
        y += titleSize + 10f

        // Address and phone
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
        y += 20f

        // Divider
        val linePaint = Paint().apply { color = Color.BLACK; strokeWidth = 2f }
        canvas.drawLine(margin, y, width - margin, y, linePaint)
        y += if (isReceipt) 30f else 50f

        // Title
        blackPaint.textSize = titleSize
        blackPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        drawCenteredText(canvas, blackPaint, "إشعار دفع", y, width)
        y += titleSize + 20f

        // Notification box (light yellow background)
        val boxPaint = Paint().apply { color = Color.rgb(255, 253, 231); style = Paint.Style.FILL }
        val boxBorder = Paint().apply { color = Color.rgb(245, 176, 65); style = Paint.Style.STROKE; strokeWidth = 3f }
        val boxTop = y - 20f
        val boxBottom = y + lineGap * 5 + 30f
        val rect = RectF(margin, boxTop, width - margin, boxBottom)
        canvas.drawRoundRect(rect, 12f, 12f, boxPaint)
        canvas.drawRoundRect(rect, 12f, 12f, boxBorder)

        // Subscriber info inside box
        blackPaint.textSize = bodySize
        blackPaint.typeface = Typeface.DEFAULT
        drawRightAlignedText(canvas, blackPaint, "المشترك: ${subscriber.fullName}", width - margin - 20f, y)
        y += lineGap
        drawRightAlignedText(canvas, blackPaint, "العداد: ${subscriber.meterNumber}", width - margin - 20f, y)
        y += lineGap
        drawRightAlignedText(canvas, blackPaint, "رقم الفاتورة: ${invoice.id}", width - margin - 20f, y)
        y += lineGap + 10f

        // Amount due (highlighted)
        blackPaint.textSize = bodySize + 6f
        blackPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        blackPaint.color = Color.rgb(191, 54, 12)
        drawRightAlignedText(canvas, blackPaint, "المبلغ المستحق: ${formatAmount(invoice.totalAmount)} درهم", width - margin - 20f, y)
        blackPaint.color = Color.BLACK
        y += lineGap + 20f

        // Payment deadline
        if (invoice.dueDate > 0) {
            val dueDate = Instant.fromEpochMilliseconds(invoice.dueDate)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
            val dueBoxPaint = Paint().apply { color = Color.rgb(255, 243, 224); style = Paint.Style.FILL }
            val dueBorder = Paint().apply { color = Color.rgb(230, 81, 0); style = Paint.Style.STROKE; strokeWidth = 3f }
            val dueBoxH = if (isReceipt) 70f else 90f
            val dueRect = RectF(margin, y, width - margin, y + dueBoxH)
            canvas.drawRoundRect(dueRect, 8f, 8f, dueBoxPaint)
            canvas.drawRoundRect(dueRect, 8f, 8f, dueBorder)
            blackPaint.color = Color.rgb(191, 54, 12)
            blackPaint.textSize = bodySize
            blackPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            drawCenteredText(canvas, blackPaint, "اجل الدفع: ${dueDate.date}", y + dueBoxH / 2 + bodySize / 3, width)
            blackPaint.color = Color.BLACK
            y += dueBoxH + (if (isReceipt) 24f else 40f)
        }

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
