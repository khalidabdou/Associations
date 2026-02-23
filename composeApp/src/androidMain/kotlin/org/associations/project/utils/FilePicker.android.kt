package org.associations.project.utils

actual object FilePicker {
    actual fun pickDirectory(): String? {
        // Android file picking requires ActivityResultContract (lifecycle-aware, async)
        // This stub unblocks the build; integrate with Activity-based picker if needed
        return null
    }

    actual fun pickFile(): String? {
        // Android file picking requires ActivityResultContract (lifecycle-aware, async)
        return null
    }
}
