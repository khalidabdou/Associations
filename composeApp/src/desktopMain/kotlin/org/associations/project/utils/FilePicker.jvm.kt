package org.associations.project.utils

import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

actual object FilePicker {
    actual fun pickDirectory(): String? {
        // We need to run this on the EDT or similar, but for simple blocking usually JFileChooser
        // works fine.
        // However, in Compose Desktop, it's safer to ensure UI interactions.
        // For simplicity here, we'll try direct invocation.
        val chooser = JFileChooser()
        chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        val result = chooser.showOpenDialog(null)
        return if (result == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile.absolutePath
        } else {
            null
        }
    }

    actual fun pickFile(): String? {
        val chooser = JFileChooser()
        chooser.fileSelectionMode = JFileChooser.FILES_ONLY
        // Optional: Filter for .db files
        val filter = FileNameExtensionFilter("Database Files", "db", "sqlite")
        chooser.fileFilter = filter

        val result = chooser.showOpenDialog(null)
        return if (result == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile.absolutePath
        } else {
            null
        }
    }
}
