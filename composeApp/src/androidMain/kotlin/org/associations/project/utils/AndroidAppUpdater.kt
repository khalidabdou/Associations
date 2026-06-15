package org.associations.project.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidAppUpdater : AppUpdater {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    override val state: StateFlow<UpdateState> = _state.asStateFlow()

    override fun checkForUpdates(manual: Boolean) {
        if (manual) {
            _state.value = UpdateState.UpToDate
        }
    }

    override fun downloadAndInstallUpdate() {
        // No-op on Android, updates managed by Google Play Store
    }

    override fun clearState() {
        _state.value = UpdateState.Idle
    }
}
