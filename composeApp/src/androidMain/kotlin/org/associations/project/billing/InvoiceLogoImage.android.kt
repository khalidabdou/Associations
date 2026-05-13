package org.associations.project.billing

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.io.File

@Composable
actual fun InvoiceLogoImage(logoPath: String, modifier: Modifier) {
    val bitmap = try {
        val file = File(logoPath)
        if (file.exists()) {
            BitmapFactory.decodeFile(logoPath)?.asImageBitmap()
        } else {
            null
        }
    } catch (e: Exception) {
        println("Error loading logo image: ${e.message}")
        null
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Association Logo",
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    }
}
