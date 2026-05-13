package org.associations.project.utils

import androidx.compose.runtime.Composable

/**
 * Returns a launcher function that opens the platform image picker.
 * On result, calls [onResult] with the absolute file path (or null if cancelled).
 * On Android, the picked image is copied to internal storage first.
 */
@Composable
expect fun rememberImagePickerLauncher(onResult: (String?) -> Unit): () -> Unit
