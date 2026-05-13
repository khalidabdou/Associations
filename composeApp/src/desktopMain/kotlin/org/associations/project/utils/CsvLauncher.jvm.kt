package org.associations.project.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.io.FileInputStream
import java.io.InputStream
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun rememberCsvImportLauncher(
    onImport: (InputStream) -> Result<String>,
    onMessage: (String) -> Unit,
): CsvLauncher = remember {
    CsvLauncher(
        import = {
            val chooser = JFileChooser()
            chooser.dialogTitle = "استيراد قراءات من CSV"
            chooser.fileFilter = FileNameExtensionFilter("CSV Files", "csv", "txt")
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
