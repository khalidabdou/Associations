package org.associations.project.utils

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.InputStream

@Composable
actual fun rememberCsvImportLauncher(
    onImport: (InputStream) -> Result<String>,
    onMessage: (String) -> Unit,
): CsvLauncher {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
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

    return remember {
        CsvLauncher(
            import = {
                launcher.launch(
                    arrayOf(
                        "text/csv",
                        "text/comma-separated-values",
                        "application/csv",
                        "text/plain",
                        "*/*"
                    )
                )
            }
        )
    }
}
