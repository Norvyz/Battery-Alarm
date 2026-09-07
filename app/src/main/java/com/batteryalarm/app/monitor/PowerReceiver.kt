package com.batteryalarm.app.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.batteryalarm.app.data.Settings

/**
 * Se registra en el manifest (Android antiguos) y también en tiempo de
 * ejecución desde [com.batteryalarm.app.BatteryAlarmApplication] (Android modernos).
 *
 * Si el usuario activó la opción, conectar el cargador inicia el monitoreo solo.
 * Al desconectarlo, detiene cualquier monitoreo activo.
 */
class PowerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val ctx = context ?: return
        when (intent?.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                if (Settings.autoStart(ctx)) {
                    try {
                        MonitoringService.start(ctx)
                    } catch (t: Throwable) {
                        Log.w(TAG, "No se pudo iniciar el monitoreo automático: $t")
                    }
                }
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                MonitoringService.stop(ctx)
            }
        }
    }

    companion object {
        private const val TAG = "BatteryAlarm"
    }
}