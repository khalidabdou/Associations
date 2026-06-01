package org.associations.project.utils

import androidx.compose.runtime.Composable
import java.io.OutputStream

/**
 * Platform-specific launcher for exporting the monthly report as a PDF.
 *
 * On Android this uses the Storage Access Framework (ActivityResultContracts.CreateDocument)
 * so the user can pick the Downloads folder (or any location) from the system file picker.
 *
 * On Desktop it falls back to Swing JFileChooser.
 */
class ReportExportLauncher(
    val export: () -> Unit,
)

@Composable
expect fun rememberReportExportLauncher(
    suggestedFileName: String,
    onExport: suspend (OutputStream) -> Unit,
    onMessage: (String) -> Unit,
): ReportExportLauncher
