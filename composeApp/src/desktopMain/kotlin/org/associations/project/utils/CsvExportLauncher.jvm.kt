package org.associations.project.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun rememberCsvExportLauncher(
    suggestedFileName: String,
    onExport: suspend (OutputStream) -> Unit,
    onMessage: (String) -> Unit,
): CsvExportLauncher = remember(suggestedFileName) {
    CsvExportLauncher(
        export = {
            val chooser = JFileChooser()
            chooser.dialogTitle = "تصدير CSV"
            chooser.selectedFile = File(suggestedFileName)
            chooser.fileFilter = FileNameExtensionFilter("CSV Files", "csv")
            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                try {
                    var file = chooser.selectedFile
                    if (!file.name.endsWith(".csv", ignoreCase = true)) {
                        file = File(file.absolutePath + ".csv")
                    }
                    FileOutputStream(file).use { out ->
                        runBlocking { onExport(out) }
                    }
                    onMessage("تم تصدير CSV بنجاح")
                } catch (e: Exception) {
                    onMessage("فشل تصدير CSV: ${e.message}")
                }
            } else {
                onMessage("تم إلغاء التصدير")
            }
        }
    )
}
