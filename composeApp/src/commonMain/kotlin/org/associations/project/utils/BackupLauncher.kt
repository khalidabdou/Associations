package org.associations.project.utils

import androidx.compose.runtime.Composable
import java.io.InputStream
import java.io.OutputStream

/**
 * Platform-specific launcher for exporting/importing the app database backup.
 *
 * On Android this uses the Storage Access Framework (ActivityResultContracts.CreateDocument /
 * OpenDocument) so the user can pick the Downloads folder (or any location) from the system
 * file picker — no storage permissions required on modern Android.
 *
 * On Desktop it falls back to Swing JFileChooser.
 */
class BackupLauncher(
    val export: () -> Unit,
    val import: () -> Unit,
)

@Composable
expect fun rememberBackupLauncher(
    suggestedFileName: String,
    onExport: (OutputStream) -> Result<String>,
    onImport: (InputStream) -> Result<String>,
    onMessage: (String) -> Unit,
): BackupLauncher
