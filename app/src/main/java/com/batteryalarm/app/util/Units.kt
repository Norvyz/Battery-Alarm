package com.batteryalarm.app.util

import java.util.Locale

object Units {
    fun formatRemaining(totalSeconds: Int): String {
        val s = totalSeconds.coerceAtLeast(0)
        return String.format(Locale.getDefault(), "%d:%02d", s / 60, s % 60)
    }

    /** Formatea una duración en segundos: "mm:ss" o "h:mm:ss". */
    fun formatDuration(totalSeconds: Long): String {
        val s = totalSeconds.coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, sec)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", m, sec)
        }
    }
}