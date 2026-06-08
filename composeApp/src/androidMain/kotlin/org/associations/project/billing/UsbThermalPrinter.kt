package org.associations.project.billing

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Handles USB connection and ESC/POS bitmap printing to thermal printers.
 *
 * Uses Android's USB Host API to discover, connect, and send ESC/POS raster
 * commands (GS v 0) directly to a USB-connected thermal printer.
 */
object UsbThermalPrinter {

    /** Mutex to serialize USB print jobs — prevents concurrent access to the same device. */
    private val printMutex = Mutex()

    /** Permission request action — must be unique per app. */
    private const val ACTION_USB_PERMISSION = "org.associations.project.USB_PERMISSION"

    // ── ESC/POS commands (same as Bluetooth) ──

    private val ESC_INIT = byteArrayOf(0x1B, 0x40)

    /** Build the GS v 0 raster command from a bitmap. */
    private fun buildRasterCommand(bitmap: Bitmap, scale: Int = 0): ByteArray {
        val maxWidth = 576
        val scaledBmp = if (bitmap.width > maxWidth) {
            val ratio = maxWidth.toFloat() / bitmap.width
            Bitmap.createScaledBitmap(bitmap, maxWidth, (bitmap.height * ratio).toInt(), true)
        } else {
            bitmap
        }

        val w = scaledBmp.width
        val h = scaledBmp.height
        val widthBytes = (w + 7) / 8

        val header = byteArrayOf(
            0x1D, 0x76, 0x30,
            scale.coerceIn(0, 3).toByte(),
            (widthBytes % 256).toByte(),
            (widthBytes / 256).toByte(),
            (h % 256).toByte(),
            (h / 256).toByte()
        )

        val pixels = IntArray(w * h)
        scaledBmp.getPixels(pixels, 0, w, 0, 0, w, h)

        val rasterData = ByteArray(widthBytes * h)
        for (y in 0 until h) {
            val rowOffset = y * widthBytes
            for (x in 0 until w) {
                val pixel = pixels[y * w + x]
                val gray = ((pixel shr 16 and 0xFF) * 0.299 +
                            (pixel shr 8 and 0xFF) * 0.587 +
                            (pixel and 0xFF) * 0.114).toInt()
                if (gray < 180) {
                    val byteIndex = rowOffset + (x / 8)
                    val bitIndex = 7 - (x % 8)
                    rasterData[byteIndex] = (rasterData[byteIndex].toInt() or (1 shl bitIndex)).toByte()
                }
            }
        }

        return header + rasterData
    }

    // ── Public API ──

    /**
     * Returns a list of currently connected USB devices that look like thermal printers.
     * Filters by USB class 7 (printer) or common vendor IDs used by POS printers.
     */
    fun getConnectedDevices(context: Context): List<UsbPrinterInfo> {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager ?: return emptyList()

        val printerClassDevices = mutableListOf<UsbPrinterInfo>()

        for ((_, device) in usbManager.deviceList) {
            // Check if any interface has printer class (7)
            val hasPrinterInterface = (0 until device.interfaceCount).any { i ->
                device.getInterface(i).interfaceClass == UsbConstants.USB_CLASS_PRINTER
            }

            // Also check common POS printer vendor IDs
            val isPosVendor = device.vendorId in setOf(
                0x0416, // Winbond
                0x0493, // Citizen
                0x04B8, // Seiko Epson
                0x0519, // Star Micronics
                0x067B, // Prolific (common USB-serial)
                0x0FE6, // ICS Advent
                0x1504, // Bixolon
                0x1FC9, // NXP
                0x28E9, // Gprinter
                0x2DD6, // Xprinter
            )

            if (hasPrinterInterface || isPosVendor) {
                val name = device.productName ?: device.deviceName ?: "USB Printer"
                printerClassDevices.add(
                    UsbPrinterInfo(
                        deviceName = name,
                        deviceId = device.deviceId,
                        vendorId = device.vendorId,
                        productId = device.productId
                    )
                )
            }
        }

        return printerClassDevices
    }

    /**
     * Requests USB permission for the given device and returns true if granted.
     * Shows a system dialog to the user if permission isn't already granted.
     */
    private suspend fun requestPermission(context: Context, device: UsbDevice): Boolean {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager ?: return false

        if (usbManager.hasPermission(device)) return true

        return suspendCancellableCoroutine { continuation ->
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    if (ACTION_USB_PERMISSION == intent.action) {
                        @Suppress("DEPRECATION")
                        val grantedDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) as? UsbDevice
                        val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                        if (grantedDevice?.deviceId == device.deviceId) {
                            context.unregisterReceiver(this)
                            if (continuation.isActive) {
                                continuation.resume(granted)
                            }
                        }
                    }
                }
            }

            val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                Context.RECEIVER_NOT_EXPORTED
            } else {
                0
            }
            context.registerReceiver(receiver, IntentFilter(ACTION_USB_PERMISSION), flags)

            val permissionIntent = PendingIntent.getBroadcast(
                context, 0,
                Intent(ACTION_USB_PERMISSION).setPackage(context.packageName),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            usbManager.requestPermission(device, permissionIntent)

            continuation.invokeOnCancellation {
                try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
            }
        }
    }

    /**
     * Finds a UsbDevice by deviceId. Returns null if not found.
     */
    private fun findDevice(context: Context, deviceId: Int): UsbDevice? {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager ?: return null
        return usbManager.deviceList.values.find { it.deviceId == deviceId }
    }

    /**
     * Prints a bitmap to the USB thermal printer with the given deviceId.
     *
     * @param context Android context
     * @param bitmap The invoice/notification bitmap to print
     * @param deviceId USB device ID (from UsbPrinterInfo)
     * @return Result indicating success or failure
     */
    suspend fun printBitmap(
        context: Context,
        bitmap: Bitmap,
        deviceId: Int
    ): Result<Unit> = printMutex.withLock {
        withContext(Dispatchers.IO) {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
                ?: return@withContext Result.failure(Exception("جهازك لا يدعم USB Host"))

            val device = findDevice(context, deviceId)
                ?: return@withContext Result.failure(Exception("الطابعة غير متصلة. تأكد من توصيل كابل USB"))

            // Request permission (shows dialog if needed)
            val hasPermission = requestPermission(context, device)
            if (!hasPermission) {
                return@withContext Result.failure(Exception("تم رفض إذن USB للطابعة"))
            }

            val connection: UsbDeviceConnection = usbManager.openDevice(device)
                ?: return@withContext Result.failure(Exception("تعذر فتح اتصال USB مع الطابعة"))

            try {
                // Find the first printer interface with a bulk-out endpoint
                var printerInterface: UsbInterface? = null
                var bulkOutEndpoint: UsbEndpoint? = null

                for (i in 0 until device.interfaceCount) {
                    val iface = device.getInterface(i)
                    for (j in 0 until iface.endpointCount) {
                        val ep = iface.getEndpoint(j)
                        if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK &&
                            ep.direction == UsbConstants.USB_DIR_OUT) {
                            printerInterface = iface
                            bulkOutEndpoint = ep
                            break
                        }
                    }
                    if (bulkOutEndpoint != null) break
                }

                if (printerInterface == null || bulkOutEndpoint == null) {
                    return@withContext Result.failure(Exception("لم يتم العثور على واجهة USB للطابعة"))
                }

                connection.claimInterface(printerInterface, true)

                // Init printer
                connection.bulkTransfer(bulkOutEndpoint, ESC_INIT, ESC_INIT.size, 1000)

                // Send raster image in chunks
                val rasterCmd = buildRasterCommand(bitmap)
                val chunkSize = 4096
                var offset = 0
                while (offset < rasterCmd.size) {
                    val len = minOf(chunkSize, rasterCmd.size - offset)
                    val chunk = rasterCmd.copyOfRange(offset, offset + len)
                    connection.bulkTransfer(bulkOutEndpoint, chunk, chunk.size, 2000)
                    offset += len
                    if (offset < rasterCmd.size) {
                        Thread.sleep(20)
                    }
                }

                // Feed paper and cut
                connection.bulkTransfer(bulkOutEndpoint, byteArrayOf(0x1B, 0x64, 6), 3, 1000)
                connection.bulkTransfer(bulkOutEndpoint, byteArrayOf(0x1D, 0x56, 1), 3, 1000)

                Thread.sleep(1500)

                connection.releaseInterface(printerInterface)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(Exception("فشل الطباعة عبر USB: ${e.message}"))
            } finally {
                try { connection.close() } catch (_: Exception) {}
            }
        }
    }
}
