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
    val windowsDownloadUrl: String,
    val macDownloadUrl: String,
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
                        val osName = System.getProperty("os.name").lowercase()
                        val downloadUrl = if (osName.contains("win")) {
                            metadata.windowsDownloadUrl
                        } else {
                            metadata.macDownloadUrl
                        }
                        _state.value = UpdateState.UpdateAvailable(
                            latestVersion = metadata.latestVersion,
                            changelog = metadata.changelog,
                            downloadUrl = downloadUrl
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
                    
                    val extension = when {
                        currentState.downloadUrl.lowercase().contains(".msi") -> "msi"
                        currentState.downloadUrl.lowercase().contains(".dmg") -> "dmg"
                        else -> "exe"
                    }
                    // Do NOT call deleteOnExit() — System.exit(0) would delete the file
                    // before msiexec / the bash script can use it. Installers clean up themselves.
                    val tempFile = File.createTempFile("Associations-Update-", ".$extension")

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
            when {
                osName.contains("mac") -> installMacOS(file)
                osName.contains("win") -> installWindows(file)
                else -> {
                    ProcessBuilder("xdg-open", file.absolutePath).start()
                    System.exit(0)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _state.value = UpdateState.Error("فشل بدء برنامج التثبيت: ${e.localizedMessage}")
        }
    }

    /**
     * macOS: writes a background shell script that waits for this process to exit,
     * mounts the DMG, copies the .app to /Applications, unmounts, and re-launches.
     */
    private fun installMacOS(dmgFile: File) {
        val appPath = findCurrentAppPath()
        val dmgPath = dmgFile.absolutePath

        val scriptFile = File.createTempFile("associations_updater_", ".sh")
        scriptFile.setExecutable(true)

        // Build script with string concatenation — triple-quoted + trimIndent adds leading spaces
        // that break bash. Also $$ must be written as literal characters for bash PID.
        val nl = "\n"
        val script = "#!/bin/bash$nl" +
            "sleep 3$nl" +
            "DMG=\"$dmgPath\"$nl" +
            "MOUNT_POINT=\"/Volumes/AssociationsUpdate_\$\$\"$nl" +
            "hdiutil attach \"\$DMG\" -mountpoint \"\$MOUNT_POINT\" -nobrowse -quiet$nl" +
            "if [ \$? -ne 0 ]; then$nl" +
            "  osascript -e 'display dialog \"فشل تثبيت التحديث: خطأ في تحميل DMG.\" buttons {\"حسناً\"} default button 1'$nl" +
            "  exit 1$nl" +
            "fi$nl" +
            "APP_IN_DMG=\$(find \"\$MOUNT_POINT\" -maxdepth 1 -name \"*.app\" | head -1)$nl" +
            "if [ -z \"\$APP_IN_DMG\" ]; then$nl" +
            "  hdiutil detach \"\$MOUNT_POINT\" -quiet$nl" +
            "  osascript -e 'display dialog \"فشل تثبيت التحديث: لم يتم العثور على التطبيق داخل DMG.\" buttons {\"حسناً\"} default button 1'$nl" +
            "  exit 1$nl" +
            "fi$nl" +
            "INSTALL_DIR=\"$appPath\"$nl" +
            "rm -rf \"\$INSTALL_DIR\"$nl" +
            "cp -R \"\$APP_IN_DMG\" \"\$INSTALL_DIR\"$nl" +
            "hdiutil detach \"\$MOUNT_POINT\" -quiet$nl" +
            "rm -f \"\$DMG\"$nl" +
            "rm -f \"\$0\"$nl" +   // self-delete script
            "open \"\$INSTALL_DIR\"$nl"

        scriptFile.writeText(script)

        // Launch script fully detached, then exit this process
        ProcessBuilder("bash", scriptFile.absolutePath)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()

        System.exit(0)
    }

    /**
     * Windows: writes a .bat launcher that waits 2s (for JVM to exit) then runs msiexec silently.
     * This avoids the race where the JVM is still running when msiexec tries to replace locked files.
     */
    private fun installWindows(msiFile: File) {
        val msiPath = msiFile.absolutePath
        val batFile = File.createTempFile("assoc_updater_", ".bat")
        // \r\n required for Windows batch files
        batFile.writeText(
            "@echo off\r\n" +
            "timeout /t 2 /nobreak >nul\r\n" +
            "msiexec.exe /i \"$msiPath\" /passive /norestart\r\n" +
            "del /f /q \"%~f0\"\r\n"  // self-delete the bat after install
        )
        // Launch batch minimized so no console window appears
        ProcessBuilder("cmd.exe", "/c", "start", "", "/min", batFile.absolutePath).start()
        System.exit(0)
    }

    /**
     * Finds the current running .app path on macOS.
     * Falls back to /Applications/Associations.app if not determinable.
     */
    private fun findCurrentAppPath(): String {
        // Try to find via the java.home path (inside the .app bundle)
        val javaHome = System.getProperty("java.home") ?: ""
        if (javaHome.contains(".app")) {
            val appEnd = javaHome.indexOf(".app") + 4
            return javaHome.substring(0, appEnd)
        }
        // Fallback: standard Applications directory
        return "/Applications/Associations.app"
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
