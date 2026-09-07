package com.batteryalarm.app.util

import java.util.Locale

object Units {
    private val zeroPad = Locale.getDefault()

    fun formatRemaining(totalSeconds: Int): String {
        val s = totalSeconds.coerceAtLeast(0)
        return String.format("%d:%02d", s / 60, s % 60)
    }
}