package org.associations.project.utils

expect object FilePicker {
    fun pickDirectory(): String?
    fun pickFile(): String?
}
