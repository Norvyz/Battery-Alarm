package com.batteryalarm.app.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
                    // En Android 12+ el arranque en segundo plano puede estar
                    // bloqueado; si ocurre, se avisa al usuario con una
                    // notificación que permite iniciar el monitoreo con un toque.
                    MonitoringService.startFromBackground(ctx)
                }
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                MonitoringService.stop(ctx)
            }
        }
    }
}