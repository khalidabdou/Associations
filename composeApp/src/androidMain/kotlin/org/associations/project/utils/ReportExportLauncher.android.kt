package org.associations.project.utils

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import java.io.OutputStream

@Composable
actual fun rememberReportExportLauncher(
    suggestedFileName: String,
    onExport: suspend (OutputStream) -> Unit,
    onMessage: (String) -> Unit,
): ReportExportLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri == null) {
            onMessage("تم إلغاء التصدير")
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            try {
                val out = context.contentResolver.openOutputStream(uri)
                    ?: throw IllegalStateException("تعذر فتح الملف للكتابة")
                out.use { onExport(it) }
                onMessage("تم تصدير التقرير بنجاح")
            } catch (e: Exception) {
                onMessage("فشل تصدير التقرير: ${e.message}")
            }
        }
    }

    return remember(suggestedFileName) {
        ReportExportLauncher(
            export = { exportLauncher.launch(suggestedFileName) }
        )
    }
}
