package org.associations.project.utils

import androidx.compose.runtime.Composable
import java.io.InputStream

/**
 * Platform launcher for picking a CSV file (text/csv) for import.
 * onImport receives the InputStream of the chosen file and returns a
 * user-facing Result message.
 */
class CsvLauncher(val import: () -> Unit)

@Composable
expect fun rememberCsvImportLauncher(
    onImport: (InputStream) -> Result<String>,
    onMessage: (String) -> Unit,
): CsvLauncher
