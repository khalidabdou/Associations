package org.associations.project.billing

/**
 * Info about a connected USB thermal printer.
 */
data class UsbPrinterInfo(
    val deviceName: String,
    val deviceId: Int,       // USB device ID (hashCode of device)
    val vendorId: Int,
    val productId: Int
)
