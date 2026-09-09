package com.batteryalarm.app.monitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.batteryalarm.app.MainActivity
import com.batteryalarm.app.R
import com.batteryalarm.app.data.Settings
import com.batteryalarm.app.util.Units

object Notifications {
    const val CHANNEL_MONITORING = "monitoring"
    const val CHANNEL_ALARM = "alarm"
    const val CHANNEL_AUTOSTART = "autostart"

    const val ID_MONITORING = 9501
    const val ID_ALARM = 9502
    const val ID_AUTOSTART = 9503

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)

        val monitoring = NotificationChannel(
            CHANNEL_MONITORING,
            context.getString(R.string.channel_monitoring),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.channel_monitoring_desc)
        }
        nm.createNotificationChannel(monitoring)

        val alarm = NotificationChannel(
            CHANNEL_ALARM,
            context.getString(R.string.channel_alarm),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_alarm_desc)
            enableVibration(true)
            setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION), null)
        }
        nm.createNotificationChannel(alarm)

        val autostart = NotificationChannel(
            CHANNEL_AUTOSTART,
            context.getString(R.string.channel_autostart),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.channel_autostart_desc)
        }
        nm.createNotificationChannel(autostart)
    }

    fun monitoringNotification(
        context: Context,
        state: MonitorState
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = MonitoringService.stopIntent(context)

        val title = context.getString(R.string.app_name)
        val text = when {
            state.alarmed -> context.getString(
                R.string.notif_complete,
                state.targetLevel
            )
            state.waiting -> context.getString(
                R.string.notif_waiting,
                state.targetLevel,
                Units.formatRemaining(state.remainingSeconds)
            )
            state.charging -> context.getString(
                R.string.notif_charging,
                state.level
            )
            else -> context.getString(R.string.notif_waiting_plug, state.level)
        }

        // Añade la información extra de la carga (voltaje, corriente, potencia,
        // temperatura, tiempo estimado) solo mientras se está cargando.
        val details = chargeDetailLines(context, state)
        val bigText = if (details.isNotEmpty()) {
            text + "\n" + details.joinToString("\n")
        } else {
            text
        }

        return NotificationCompat.Builder(context, CHANNEL_MONITORING)
            .setSmallIcon(R.drawable.ic_stat_bolt)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(contentIntent)
            .addAction(
                R.drawable.ic_stop,
                context.getString(R.string.notif_action_stop),
                stopIntent
            )
            .build()
    }

    /**
     * Líneas extra de la notificación con la información de la carga.
     * Solo muestra datos realmente disponibles; si un teléfono no reporta
     * corriente o temperatura, esa línea simplemente no aparece (o queda
     * "No disponible" cuando corresponde).
     */
    private fun chargeDetailLines(context: Context, state: MonitorState): List<String> {
        if (!state.charging) return emptyList()
        val r = state.reading
        val lines = mutableListOf<String>()

        val v = r.voltageText()
        val c = r.currentText()
        when {
            v != null && c != null -> lines.add(
                context.getString(R.string.charge_voltage_v, v) + " · " +
                    context.getString(R.string.charge_current_a, c)
            )
            v != null -> lines.add(context.getString(R.string.charge_voltage_v, v))
            c != null -> lines.add(context.getString(R.string.charge_current_a, c))
        }

        val power = r.powerText()
        if (power != null) {
            lines.add(context.getString(R.string.charge_power_w, power))
        } else if (v != null) {
            lines.add(context.getString(R.string.charge_power_na))
        }

        val temp = r.tempText()
        if (temp != null) {
            lines.add(context.getString(R.string.charge_temp_c, temp))
        }

        // Tiempo estimado al 100%
        val etaMinutes = r.estimateMinutes
        val etaText = when {
            etaMinutes == null -> context.getString(R.string.charge_calculating)
            etaMinutes < 60 ->
                context.getString(R.string.charge_min, etaMinutes)
            else ->
                context.getString(R.string.charge_hour_min, etaMinutes / 60, etaMinutes % 60)
        }
        lines.add(context.getString(R.string.charge_eta_label, etaText))

        return lines
    }

    /** Aviso cuando no se pudo iniciar el monitoreo en segundo plano (Android 12+). */
    fun showAutoStartNotice(context: Context) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val contentIntent = PendingIntent.getActivity(
            context,
            2,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val startIntent = MonitoringService.startIntent(context)
        val body = context.getString(R.string.autostart_notice_body)
        val notification = NotificationCompat.Builder(context, CHANNEL_AUTOSTART)
            .setSmallIcon(R.drawable.ic_stat_bolt)
            .setContentTitle(context.getString(R.string.autostart_notice_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_bolt,
                context.getString(R.string.autostart_notice_action),
                startIntent
            )
            .build()
        try {
            NotificationManagerCompat.from(context).notify(ID_AUTOSTART, notification)
        } catch (_: SecurityException) {
            // Permiso de notificaciones denegado; no hay forma de avisar.
        }
    }

    fun alarmNotification(
        context: Context,
        state: MonitorState
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            1,
            MainActivity.alarmIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = MonitoringService.stopIntent(context)

        val title = context.getString(R.string.alarm_title)
        val text = context.getString(
            R.string.alarm_body,
            state.delayMinutes,
            context.getString(if (state.delayMinutes == 1) R.string.minuto else R.string.minutos),
            state.targetLevel
        )

        return NotificationCompat.Builder(context, CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_stat_bolt)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(contentIntent)
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                R.drawable.ic_stop,
                context.getString(R.string.notif_action_stop),
                stopIntent
            )
            .build()
    }

    fun showMonitoring(context: Context, state: MonitorState) {
        notifyIfAllowed(context, ID_MONITORING, monitoringNotification(context, state))
    }

    fun showAlarm(context: Context, state: MonitorState) {
        notifyIfAllowed(context, ID_ALARM, alarmNotification(context, state))
    }

    fun cancelMonitoring(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID_MONITORING)
    }

    fun cancelAlarm(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID_ALARM)
    }

    private fun notifyIfAllowed(context: Context, id: Int, notification: Notification) {
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return
        try {
            nm.notify(id, notification)
        } catch (_: SecurityException) {
            // Permiso de notificaciones denegado; la notificación no se muestra.
        }
    }

    fun hasNotificationAccessDenied(context: Context): Boolean {
        val nm = NotificationManagerCompat.from(context)
        return !nm.areNotificationsEnabled()
    }

    fun requestPermissionNeeded(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}