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
actual fun rememberReportExportLauncher(
    suggestedFileName: String,
    onExport: suspend (OutputStream) -> Unit,
    onMessage: (String) -> Unit,
): ReportExportLauncher = remember(suggestedFileName) {
    ReportExportLauncher(
        export = {
            val chooser = JFileChooser()
            chooser.dialogTitle = "تصدير التقرير الشهري"
            chooser.selectedFile = File(suggestedFileName)
            chooser.fileFilter = FileNameExtensionFilter("PDF Files", "pdf")
            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                try {
                    var file = chooser.selectedFile
                    if (!file.name.endsWith(".pdf", ignoreCase = true)) {
                        file = File(file.absolutePath + ".pdf")
                    }
                    FileOutputStream(file).use { out ->
                        runBlocking { onExport(out) }
                    }
                    onMessage("تم تصدير التقرير بنجاح")
                } catch (e: Exception) {
                    onMessage("فشل تصدير التقرير: ${e.message}")
                }
            } else {
                onMessage("تم إلغاء التصدير")
            }
        }
    )
}
