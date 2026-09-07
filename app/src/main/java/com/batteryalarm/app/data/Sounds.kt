package com.batteryalarm.app.data

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import com.batteryalarm.app.R

object Sounds {
    /** Resuelve la URI del sonido seleccionado, o null si no hay uno reproducible. */
    fun resolveUri(context: Context, soundKey: String): Uri? {
        return when (soundKey) {
            Settings.SOUND_SYSTEM_ALARM -> {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
            Settings.SOUND_SYSTEM_NOTIFICATION -> {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
            else -> {
                val uri = Uri.parse(soundKey)
                if (uri.scheme == "content" || uri.scheme == "file") uri else null
            }
        }
    }

    fun label(context: Context, soundKey: String): String {
        return when (soundKey) {
            Settings.SOUND_SYSTEM_ALARM ->
                context.getString(R.string.sound_system_alarm)
            Settings.SOUND_SYSTEM_NOTIFICATION ->
                context.getString(R.string.sound_system_notification)
            else -> context.getString(R.string.sound_custom)
        }
    }
}