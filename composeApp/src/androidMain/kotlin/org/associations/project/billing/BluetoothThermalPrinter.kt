package org.associations.project.billing

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.graphics.Bitmap
import java.io.OutputStream
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Handles Bluetooth connection and ESC/POS bitmap printing to thermal printers.
 *
 * Uses the standard SPP (Serial Port Profile) UUID for Bluetooth communication.
 * Renders the invoice bitmap as ESC/POS raster image commands (GS v 0).
 */
object BluetoothThermalPrinter {

    /** Standard Bluetooth SPP UUID */
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    /**
     * Mutex that ensures only one Bluetooth print job runs at a time.
     * Concurrent connections to the same printer cause "Service Discovery failed" errors.
     */
    private val printMutex = Mutex()

    // ── ESC/POS commands ──

    /** Initialize printer */
    private val ESC_INIT = byteArrayOf(0x1B, 0x40)

    /** Line feed */
    private fun ESC_LF(n: Int = 1) = ByteArray(n) { 0x0A }

    /** Print and feed paper n lines */
    private fun ESC_D(n: Int) = byteArrayOf(0x1B, 0x64, n.coerceIn(0, 255).toByte())

    /** Feed paper n lines then partial cut (most universally supported) */
    private fun GS_CUT_FEED(n: Int = 5) = byteArrayOf(0x1D, 0x56, 0x42, n.coerceIn(1, 255).toByte())

    /** Align left */
    private val ESC_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)

    /** Align center */
    private val ESC_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)

    /**
     * GS v 0 — Print raster bit image.
     *
     * Format: GS v 0 m xL xH yL yH d1...dk
     *   m  = 0 (normal), 1 (double width), 2 (double height), 3 (quadruple)
     *   xL = (widthBytes % 256)
     *   xH = (widthBytes / 256)
     *   yL = (height % 256)
     *   yH = (height / 256)
     *   d1...dk = raster data (1 bit per pixel, each row packed into bytes, MSB first)
     */
    private fun buildRasterCommand(bitmap: Bitmap, scale: Int = 0): ByteArray {
        // Scale the bitmap to fit 80mm paper (~384-576 dots at 203dpi, ~512 at 8dpmm)
        val maxWidth = 576
        val scaledBmp = if (bitmap.width > maxWidth) {
            val ratio = maxWidth.toFloat() / bitmap.width
            Bitmap.createScaledBitmap(bitmap, maxWidth, (bitmap.height * ratio).toInt(), true)
        } else {
            bitmap
        }

        val w = scaledBmp.width
        val h = scaledBmp.height
        val widthBytes = (w + 7) / 8  // bytes per row (round up)

        val header = byteArrayOf(
            0x1D, 0x76, 0x30,           // GS v 0
            scale.coerceIn(0, 3).toByte(), // m
            (widthBytes % 256).toByte(),   // xL
            (widthBytes / 256).toByte(),   // xH
            (h % 256).toByte(),            // yL
            (h / 256).toByte()             // yH
        )

        val pixels = IntArray(w * h)
        scaledBmp.getPixels(pixels, 0, w, 0, 0, w, h)

        val rasterData = ByteArray(widthBytes * h)
        for (y in 0 until h) {
            val rowOffset = y * widthBytes
            for (x in 0 until w) {
                val pixel = pixels[y * w + x]
                // RGB to grayscale: if luminance < 128, it's "black" (print a dot)
                val gray = ((pixel shr 16 and 0xFF) * 0.299 +
                            (pixel shr 8 and 0xFF) * 0.587 +
                            (pixel and 0xFF) * 0.114).toInt()
                if (gray < 180) {  // threshold — print dark pixels
                    val byteIndex = rowOffset + (x / 8)
                    val bitIndex = 7 - (x % 8)  // MSB first
                    rasterData[byteIndex] = (rasterData[byteIndex].toInt() or (1 shl bitIndex)).toByte()
                }
            }
        }

        return header + rasterData
    }

    // ── Public API ──

    /**
     * Returns a list of paired Bluetooth devices.
     * Each entry is a Pair of (name, address).
     */
    fun getPairedDevices(context: Context): List<Pair<String, String>> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()

        return adapter.bondedDevices.map { Pair(it.name ?: "Unknown", it.address) }
    }

    /**
     * Prints a bitmap to the Bluetooth thermal printer at the given address.
     *
     * @param context Android context (needed for BluetoothAdapter)
     * @param bitmap The invoice/notification bitmap to print
     * @param deviceAddress Bluetooth MAC address of the printer
     * @return Result indicating success or failure
     */
    private fun writeInChunks(outputStream: OutputStream, data: ByteArray, chunkSize: Int = 1024, delayMs: Long = 20) {
        var offset = 0
        while (offset < data.size) {
            val len = minOf(chunkSize, data.size - offset)
            outputStream.write(data, offset, len)
            outputStream.flush()
            offset += len
            if (offset < data.size) {
                Thread.sleep(delayMs)
            }
        }
    }

    /**
     * Prints a bitmap to the Bluetooth thermal printer at the given address.
     *
     * Uses [printMutex] to guarantee that only one job is active at a time —
     * concurrent Bluetooth connections to the same device cause "Service Discovery
     * failed" / "Connection refused" errors.
     *
     * @param context Android context (needed for BluetoothAdapter)
     * @param bitmap The invoice/notification bitmap to print
     * @param deviceAddress Bluetooth MAC address of the printer
     * @return Result indicating success or failure
     */
    suspend fun printBitmap(
        context: Context,
        bitmap: Bitmap,
        deviceAddress: String
    ): Result<Unit> = printMutex.withLock {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val adapter = BluetoothAdapter.getDefaultAdapter()
                ?: return@withContext Result.failure(Exception("لا يوجد محول بلوتوث"))

            val device: BluetoothDevice = try {
                adapter.getRemoteDevice(deviceAddress)
            } catch (e: IllegalArgumentException) {
                return@withContext Result.failure(Exception("عنوان الطابعة غير صحيح: $deviceAddress"))
            }

            val socket: BluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            try {
                socket.connect()

                val outputStream: OutputStream = socket.outputStream

                // Init printer
                outputStream.write(ESC_INIT)
                outputStream.flush()
                Thread.sleep(50)

                // Send raster image in chunks to prevent buffer overflow/gibberish symbols
                val rasterCmd = buildRasterCommand(bitmap)
                writeInChunks(outputStream, rasterCmd)

                // Feed paper and cut exactly once
                outputStream.write(byteArrayOf(0x1B, 0x64, 6)) // ESC d 6 (feed 6 lines)
                outputStream.write(byteArrayOf(0x1D, 0x56, 1)) // GS V 1 (partial cut, standard)
                outputStream.flush()

                // Wait for printer to finish and cut before we disconnect
                Thread.sleep(1500)

                outputStream.close()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(Exception("فشل الطباعة: ${e.message}"))
            } finally {
                // Always close the socket — prevents blocking the next print job
                try { socket.close() } catch (_: Exception) {}
            }
        }
    }
}
