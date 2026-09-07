package com.batteryalarm.app.monitor

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Estado compartido entre el servicio y la interfaz.
 * `null` significa que no hay monitoreo activo.
 */
data class MonitorState(
    val active: Boolean = false,
    val charging: Boolean = false,
    val level: Int = 0,
    val waiting: Boolean = false,
    val remainingSeconds: Int = 0,
    val alarmed: Boolean = false,
    val delayMinutes: Int = 2
)

object Monitor {
    private val _state = MutableStateFlow<MonitorState?>(null)
    val state: StateFlow<MonitorState?> = _state

    fun update(transform: (MonitorState) -> MonitorState) {
        val current = _state.value ?: MonitorState()
        _state.value = transform(current.copy(active = true))
    }

    fun reset() {
        _state.value = null
    }
}