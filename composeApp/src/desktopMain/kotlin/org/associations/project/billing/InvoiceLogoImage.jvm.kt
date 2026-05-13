package org.associations.project.billing

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.skia.Image
import java.io.File

@Composable
actual fun InvoiceLogoImage(logoPath: String, modifier: Modifier) {
    val imageBitmap = loadImageBitmap(logoPath)
    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = "Association Logo",
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    }
}

private fun loadImageBitmap(path: String): ImageBitmap? {
    return try {
        val file = File(path)
        if (file.exists()) {
            val bytes = file.readBytes()
            val skiaImage = Image.makeFromEncoded(bytes)
            skiaImage.toComposeImageBitmap()
        } else {
            null
        }
    } catch (e: Exception) {
        println("Error loading logo image: ${e.message}")
        null
    }
}
