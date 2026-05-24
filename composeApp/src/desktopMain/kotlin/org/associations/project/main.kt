package org.associations.project

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.res.painterResource
import org.associations.project.di.initKoin

fun main() = application {
    initKoin()

    Window(
        onCloseRequest = ::exitApplication,
        title = "Associations",
        icon = painterResource("icon.ico"),
    ) {
        App()
    }
}