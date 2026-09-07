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

    const val ID_MONITORING = 9501
    const val ID_ALARM = 9502

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
            state.alarmed -> context.getString(R.string.notif_complete)
            state.waiting -> context.getString(
                R.string.notif_waiting,
                Units.formatRemaining(state.remainingSeconds)
            )
            state.charging -> context.getString(
                R.string.notif_charging,
                state.level
            )
            else -> context.getString(R.string.notif_waiting_plug, state.level)
        }

        return NotificationCompat.Builder(context, CHANNEL_MONITORING)
            .setSmallIcon(R.drawable.ic_stat_bolt)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
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
            context.getString(if (state.delayMinutes == 1) R.string.minuto else R.string.minutos)
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