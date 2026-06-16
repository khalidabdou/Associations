package org.associations.project.utils

import kotlinx.coroutines.flow.StateFlow

/**
 * The current application version.
 *
 * At CI build time, BuildConfig.APP_VERSION is generated from the APP_VERSION
 * environment variable (set by the release workflow from the git tag).
 * The hardcoded fallback here is used for local development builds.
 */
val APP_VERSION: String by lazy {
    try {
        BuildConfig.APP_VERSION
    } catch (_: NoClassDefFoundError) {
        "1.0.11"
    }
}

interface AppUpdater {
    val state: StateFlow<UpdateState>
    fun checkForUpdates(manual: Boolean = false)
    fun downloadAndInstallUpdate()
    /** Launch the downloaded installer (after ReadyToInstall state is reached). */
    fun triggerInstall()
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
