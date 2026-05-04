package org.associations.project.utils

import androidx.compose.runtime.Composable

@Composable
actual fun rememberImagePickerLauncher(onResult: (String?) -> Unit): () -> Unit {
    return {
        val path = FilePicker.pickImageFile()
        onResult(path)
    }
}
