package org.associations.project.utils

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.InputStream
import java.io.OutputStream

@Composable
actual fun rememberBackupLauncher(
    suggestedFileName: String,
    onExport: (OutputStream) -> Result<String>,
    onImport: (InputStream) -> Result<String>,
    onMessage: (String) -> Unit,
): BackupLauncher {
    val context = LocalContext.current

    // Export: use CreateDocument so the user chooses Downloads (or any folder) via SAF.
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri == null) {
            onMessage("تم إلغاء التصدير")
            return@rememberLauncherForActivityResult
        }
        try {
            val out = context.contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException("تعذر فتح الملف للكتابة")
            val result = out.use { onExport(it) }
            result.fold(
                onSuccess = { msg -> onMessage(msg) },
                onFailure = { e -> onMessage("فشل التصدير: ${e.message}") }
            )
        } catch (e: Exception) {
            onMessage("فشل التصدير: ${e.message}")
        }
    }

    // Import: use OpenDocument to let the user pick the backup file.
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            onMessage("تم إلغاء الاستيراد")
            return@rememberLauncherForActivityResult
        }
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("تعذر فتح الملف للقراءة")
            val result = input.use { onImport(it) }
            result.fold(
                onSuccess = { msg -> onMessage(msg) },
                onFailure = { e -> onMessage("فشل الاستيراد: ${e.message}") }
            )
        } catch (e: Exception) {
            onMessage("فشل الاستيراد: ${e.message}")
        }
    }

    return remember(suggestedFileName) {
        BackupLauncher(
            export = { exportLauncher.launch(suggestedFileName) },
            import = {
                importLauncher.launch(
                    arrayOf(
                        "application/octet-stream",
                        "application/x-sqlite3",
                        "application/vnd.sqlite3",
                        "*/*"
                    )
                )
            }
        )
    }
}
