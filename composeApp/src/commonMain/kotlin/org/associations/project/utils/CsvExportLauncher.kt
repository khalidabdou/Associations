package org.associations.project.utils

import androidx.compose.runtime.Composable
import java.io.OutputStream

/**
 * Platform-specific launcher for exporting data as a CSV file.
 *
 * On Android this uses the Storage Access Framework (ActivityResultContracts.CreateDocument)
 * with MIME type text/csv.
 *
 * On Desktop it falls back to Swing JFileChooser.
 */
class CsvExportLauncher(
    val export: () -> Unit,
)

@Composable
expect fun rememberCsvExportLauncher(
    suggestedFileName: String,
    onExport: suspend (OutputStream) -> Unit,
    onMessage: (String) -> Unit,
): CsvExportLauncher
