package com.batteryalarm.app.data

import android.content.Context

/** El tiempo que se espera después de llegar al 100% antes de tocar la alarma. */
object Delay {
    const val PRESET_1 = 1
    const val PRESET_2 = 2
    const val PRESET_5 = 5
    const val PRESET_10 = 10
    const val PRESET_15 = 15

    val PRESETS = intArrayOf(PRESET_1, PRESET_2, PRESET_5, PRESET_10, PRESET_15)
}

object Settings {
    private const val PREFS = "battery_alarm"
    private const val KEY_DELAY_MINUTES = "delay_minutes"
    private const val KEY_SOUND = "alarm_sound"

    const val SOUND_SYSTEM_ALARM = "system_alarm"
    const val SOUND_SYSTEM_NOTIFICATION = "system_notification"

    fun delayMinutes(context: Context): Int {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val saved = p.getInt(KEY_DELAY_MINUTES, Delay.PRESET_2)
        return if (saved in Delay.PRESET_1..Delay.PRESET_15) saved else Delay.PRESET_2
    }

    fun setDelayMinutes(context: Context, minutes: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_DELAY_MINUTES, minutes).apply()
    }

    /** null = usar sonido de alarma del sistema. */
    fun sound(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_SOUND, SOUND_SYSTEM_ALARM) ?: SOUND_SYSTEM_ALARM
    }

    fun setSound(context: Context, value: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_SOUND, value).apply()
    }
}