package com.batteryalarm.app.data

import android.content.res.Resources
import android.os.BatteryManager
import com.batteryalarm.app.R
import java.util.Locale

/**
 * Información de la batería y de la carga en un instante del monitoreo.
 *
 * Todos los valores son opcionales: muchos teléfonos no reportan corriente,
 * temperatura o el tipo exacto de conexión. `null` significa "no disponible",
 * nunca un valor inventado.
 */
data class ChargeReading(
    val pluggedType: Int = 0,
    val voltageMv: Int? = null,
    val currentMicroA: Int? = null,
    val temperatureTenths: Int? = null,
    val estimateMinutes: Int? = null
) {
    /**
     * Potencia estimada (Voltaje × Corriente). Es una aproximación: representa
     * la potencia que llega a la batería, no necesariamente la del cargador
     * de pared, y no debe presentarse como una medición certificada.
     */
    val powerWatts: Double?
        get() {
            val v = voltageMv ?: return null
            val i = currentMicroA ?: return null
            if (v <= 0 || i <= 0) return null
            return v.toDouble() * i / 1_000_000.0
        }

    fun voltageText(): String? =
        voltageMv?.takeIf { it > 0 }?.let { String.format(Locale.getDefault(), "%.1f", it / 1000.0) }

    fun currentText(): String? =
        currentMicroA?.takeIf { it > 0 }?.let { String.format(Locale.getDefault(), "%.1f", it / 1_000_000.0) }

    fun powerText(): String? =
        powerWatts?.let { String.format(Locale.getDefault(), "%.1f", it) }

    fun tempText(): String? =
        temperatureTenths?.takeIf { it != 0 }?.let { String.format(Locale.getDefault(), "%.0f", it / 10.0) }

    fun plugLabel(resources: Resources): String? = when (pluggedType) {
        BatteryManager.BATTERY_PLUGGED_AC -> resources.getString(R.string.charge_plug_ac)
        BatteryManager.BATTERY_PLUGGED_USB -> resources.getString(R.string.charge_plug_usb)
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> resources.getString(R.string.charge_plug_wireless)
        BatteryManager.BATTERY_PLUGGED_DOCK -> resources.getString(R.string.charge_plug_dock)
        else -> null
    }
}