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
    private const val KEY_AUTO_START = "auto_start_on_connect"
    private const val KEY_BOOST_LEVEL = "alarm_boost_level"
    private const val KEY_TARGET_LEVEL = "alarm_target_level"
    private const val KEY_THEME = "app_theme"

    const val SOUND_SYSTEM_ALARM = "system_alarm"
    const val SOUND_SYSTEM_NOTIFICATION = "system_notification"

    const val THEME_SYSTEM = "system"
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"

    /** Modo de tema elegido por el usuario: "system", "light" o "dark". */
    fun themeMode(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM
    }

    fun setThemeMode(context: Context, mode: String) {
        val safe = when (mode) {
            THEME_LIGHT -> THEME_LIGHT
            THEME_DARK -> THEME_DARK
            else -> THEME_SYSTEM
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_THEME, safe).apply()
    }

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

    /** true = el monitoreo se inicia solo cuando se conecta el cargador. */
    fun autoStart(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_START, false)
    }

    fun setAutoStart(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AUTO_START, enabled).apply()
    }

    /** Nivel del boost de volumen: 0 = desactivado, 1..100 = % del volumen máximo. */
    fun boostLevel(context: Context): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_BOOST_LEVEL, 0).coerceIn(0, 100)
    }

    fun setBoostLevel(context: Context, level: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_BOOST_LEVEL, level.coerceIn(0, 100)).apply()
    }

    /** true = el boost de volumen está activado. */
    fun boostEnabled(context: Context): Boolean = boostLevel(context) > 0

    /** Porcentaje (1..100) al que debe sonar la alarma. 100 = carga completa. */
    fun targetLevel(context: Context): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_TARGET_LEVEL, 100).coerceIn(1, 100)
    }

    fun setTargetLevel(context: Context, level: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_TARGET_LEVEL, level.coerceIn(1, 100)).apply()
    }
}