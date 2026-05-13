package org.associations.project.utils

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.FileOutputStream

@Composable
actual fun rememberImagePickerLauncher(onResult: (String?) -> Unit): () -> Unit {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = copyToInternalStorage(context, uri)
            onResult(path)
        } else {
            onResult(null)
        }
    }

    return {
        launcher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }
}

private fun copyToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val logoDir = File(context.filesDir, "logos")
        if (!logoDir.exists()) logoDir.mkdirs()

        val fileName = "logo_${System.currentTimeMillis()}.png"
        val outFile = File(logoDir, fileName)

        FileOutputStream(outFile).use { output ->
            inputStream.copyTo(output)
        }
        inputStream.close()

        outFile.absolutePath
    } catch (e: Exception) {
        println("Error copying image to internal storage: ${e.message}")
        null
    }
}
