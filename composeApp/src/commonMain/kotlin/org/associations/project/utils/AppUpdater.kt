package org.associations.project.utils

import kotlinx.coroutines.flow.StateFlow

const val APP_VERSION = "1.0.2"

interface AppUpdater {
    val state: StateFlow<UpdateState>
    fun checkForUpdates(manual: Boolean = false)
    fun downloadAndInstallUpdate()
    fun clearState()
}

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    object UpToDate : UpdateState()
    data class UpdateAvailable(
        val latestVersion: String,
        val changelog: String,
        val downloadUrl: String
    ) : UpdateState()
    data class Downloading(val progress: Float) : UpdateState()
    data class ReadyToInstall(val msiFilePath: String) : UpdateState()
    data class Error(val message: String) : UpdateState()
}
