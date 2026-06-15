package org.associations.project.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

@Serializable
private data class UpdateMetadata(
    val latestVersion: String,
    val downloadUrl: String,
    val changelog: String
)

class JVMAppUpdater : AppUpdater {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    override val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default)
    private val updateConfigUrl = "https://raw.githubusercontent.com/khalidabdou/Associations/main/version.json"

    override fun checkForUpdates(manual: Boolean) {
        _state.value = UpdateState.Checking
        scope.launch(Dispatchers.IO) {
            try {
                val url = URL(updateConfigUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.connect()

                if (connection.responseCode == 200) {
                    val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                    val metadata = Json.decodeFromString<UpdateMetadata>(jsonText)
                    
                    if (isNewerVersion(APP_VERSION, metadata.latestVersion)) {
                        _state.value = UpdateState.UpdateAvailable(
                            latestVersion = metadata.latestVersion,
                            changelog = metadata.changelog,
                            downloadUrl = metadata.downloadUrl
                        )
                    } else {
                        _state.value = UpdateState.UpToDate
                    }
                } else {
                    _state.value = UpdateState.Error("فشل الاتصال بالخادم: رمز الاستجابة ${connection.responseCode}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (manual) {
                    _state.value = UpdateState.Error("خطأ في التحقق من التحديثات: ${e.localizedMessage ?: "حدث خطأ غير معروف"}")
                } else {
                    _state.value = UpdateState.Idle
                }
            }
        }
    }

    override fun downloadAndInstallUpdate() {
        val currentState = _state.value
        if (currentState !is UpdateState.UpdateAvailable) return

        _state.value = UpdateState.Downloading(0f)
        scope.launch(Dispatchers.IO) {
            try {
                val url = URL(currentState.downloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.connect()

                if (connection.responseCode == 200) {
                    val contentLength = connection.contentLengthLong
                    
                    val extension = if (currentState.downloadUrl.lowercase().contains(".msi")) "msi" else "exe"
                    val tempFile = File.createTempFile("Associations-Update-", ".$extension")
                    tempFile.deleteOnExit()

                    connection.inputStream.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var totalBytesRead = 0L
                            
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                totalBytesRead += bytesRead
                                if (contentLength > 0) {
                                    val progress = totalBytesRead.toFloat() / contentLength
                                    _state.value = UpdateState.Downloading(progress)
                                }
                            }
                        }
                    }

                    _state.value = UpdateState.ReadyToInstall(tempFile.absolutePath)
                    
                    installAndExit(tempFile)
                } else {
                    _state.value = UpdateState.Error("فشل تنزيل ملف التحديث: رمز الاستجابة ${connection.responseCode}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = UpdateState.Error("خطأ أثناء تنزيل التحديث: ${e.localizedMessage}")
            }
        }
    }

    private fun installAndExit(file: File) {
        try {
            val osName = System.getProperty("os.name").lowercase()
            val process = when {
                osName.contains("win") -> {
                    ProcessBuilder("cmd.exe", "/c", "start", "", file.absolutePath).start()
                }
                osName.contains("mac") -> {
                    ProcessBuilder("open", file.absolutePath).start()
                }
                else -> {
                    ProcessBuilder("xdg-open", file.absolutePath).start()
                }
            }
            if (process != null) {
                System.exit(0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _state.value = UpdateState.Error("فشل بدء برنامج التثبيت: ${e.localizedMessage}")
        }
    }

    override fun clearState() {
        _state.value = UpdateState.Idle
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until minOf(currentParts.size, latestParts.size)) {
            if (latestParts[i] > currentParts[i]) return true
            if (currentParts[i] > latestParts[i]) return false
        }
        return latestParts.size > currentParts.size
    }
}
