package org.associations.project.security

import java.net.NetworkInterface
import java.security.MessageDigest

class DesktopDeviceFingerprint : DeviceFingerprint {
    override fun getDeviceId(): String {
        val osName = System.getProperty("os.name") ?: "unknown_os"
        val userName = System.getProperty("user.name") ?: "unknown_user"

        val macAddress =
                try {
                    val networkInterfaces = NetworkInterface.getNetworkInterfaces()
                    var mac: String? = null
                    while (networkInterfaces.hasMoreElements()) {
                        val ni = networkInterfaces.nextElement()
                        val hardwareAddress = ni.hardwareAddress
                        if (hardwareAddress != null) {
                            mac = hardwareAddress.joinToString("") { "%02X".format(it) }
                            break // specific logic might be needed to pick the *correct* interface,
                            // but this is a starter
                        }
                    }
                    mac ?: "no_mac"
                } catch (e: Exception) {
                    "mac_error"
                }

        val rawId = "$osName-$userName-$macAddress"
        return hashString(rawId)
    }

    private fun hashString(input: String): String {
        val bytes = input.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
