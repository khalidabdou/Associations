package org.associations.project.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun rememberBackupLauncher(
    suggestedFileName: String,
    onExport: (OutputStream) -> Result<String>,
    onImport: (InputStream) -> Result<String>,
    onMessage: (String) -> Unit,
): BackupLauncher = remember(suggestedFileName) {
    BackupLauncher(
            export = {
                val chooser = JFileChooser()
                chooser.dialogTitle = "تصدير النسخة الاحتياطية"
                chooser.selectedFile = File(suggestedFileName)
                chooser.fileFilter = FileNameExtensionFilter("Database Files", "db", "sqlite")
                if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                    try {
                        FileOutputStream(chooser.selectedFile).use { out ->
                            onExport(out).fold(
                                    onSuccess = { msg -> onMessage(msg) },
                                    onFailure = { e -> onMessage("فشل التصدير: ${e.message}") }
                            )
                        }
                    } catch (e: Exception) {
                        onMessage("فشل التصدير: ${e.message}")
                    }
                } else {
                    onMessage("تم إلغاء التصدير")
                }
            },
            import = {
                val chooser = JFileChooser()
                chooser.dialogTitle = "استيراد النسخة الاحتياطية"
                chooser.fileFilter = FileNameExtensionFilter("Database Files", "db", "sqlite")
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    try {
                        FileInputStream(chooser.selectedFile).use { input ->
                            onImport(input).fold(
                                    onSuccess = { msg -> onMessage(msg) },
                                    onFailure = { e -> onMessage("فشل الاستيراد: ${e.message}") }
                            )
                        }
                    } catch (e: Exception) {
                        onMessage("فشل الاستيراد: ${e.message}")
                    }
                } else {
                    onMessage("تم إلغاء الاستيراد")
                }
            }
    )
}
