package com.batteryalarm.app.monitor

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.LoudnessEnhancer
import android.os.VibrationEffect
import android.os.Vibrator
import com.batteryalarm.app.data.Settings
import com.batteryalarm.app.data.Sounds

/**
 * Reproduce la alarma de forma independiente hasta que se detenga.
 *
 * Reproduce en el flujo de alarma del sistema (STREAM_ALARM). Si el usuario
 * activa el boost, además de elevar el volumen de ese flujo se aplica una
 * ganancia real de audio ([LoudnessEnhancer]) según el nivel elegido, de modo
 * que el sonido se escucha más fuerte aunque el volumen del sistema ya esté alto.
 * Al detener la alarma se restaura el volumen original.
 */
object AlarmPlayer {
    /** Ganancia máxima del boost en millibelios (approx +10 dB a 100%). */
    private const val MAX_BOOST_MB = 2000

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var enhancer: LoudnessEnhancer? = null
    private var audioManager: AudioManager? = null
    private var prevAlarmVolume = -1
    private var boostApplied = false

    @Synchronized
    fun play(context: Context) {
        stop()
        val boostLevel = Settings.boostLevel(context)
        if (boostLevel > 0) applyBoost(context)

        val key = Settings.sound(context)
        val uri = Sounds.resolveUri(context, key)
        if (uri != null) {
            try {
                val p = MediaPlayer()
                p.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                p.setDataSource(context, uri)
                p.setOnPreparedListener {
                    it.setVolume(1.0f, 1.0f)
                    it.isLooping = true
                    it.start()
                    applyLoudness(it, boostLevel)
                }
                p.setOnErrorListener { mp, _, _ ->
                    try {
                        mp.reset()
                    } catch (_: Exception) {
                    }
                    try {
                        mp.release()
                    } catch (_: Exception) {
                    }
                    player = null
                    true
                }
                p.prepareAsync()
                player = p
            } catch (_: Exception) {
                player = null
            }
        }

        try {
            val v = context.getSystemService(Vibrator::class.java)
            if (v != null && v.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    v.vibrate(
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 400, 200, 400, 200, 600),
                            -1
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(longArrayOf(0, 400, 200, 400, 200, 600), -1)
                }
                vibrator = v
            }
        } catch (_: Exception) {
        }
    }

    @Synchronized
    fun stop() {
        try {
            player?.stop()
        } catch (_: Exception) {
        }
        try {
            player?.release()
        } catch (_: Exception) {
        }
        player = null

        try {
            enhancer?.release()
        } catch (_: Exception) {
        }
        enhancer = null

        try {
            vibrator?.cancel()
        } catch (_: Exception) {
        }
        vibrator = null

        restoreBoost()
    }

    /**
     * Aplica una ganancia real de salida al audio de la alarma. Es lo que
     * produce el "boost" de verdad: funciona incluso si el volumen del sistema
     * ya está al máximo. En dispositivos sin soporte se ignora silenciosamente.
     */
    private fun applyLoudness(player: MediaPlayer, level: Int) {
        if (level <= 0) return
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) return
        try {
            val gainMb = (MAX_BOOST_MB.toLong() * level / 100).toInt().coerceIn(0, MAX_BOOST_MB)
            val e = LoudnessEnhancer(player.audioSessionId)
            e.setTargetGain(gainMb)
            e.enabled = true
            enhancer = e
        } catch (_: Exception) {
            enhancer = null
        }
    }

    private fun applyBoost(context: Context) {
        val level = Settings.boostLevel(context)
        if (level <= 0) return
        try {
            val am = context.getSystemService(AudioManager::class.java) ?: return
            val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val target = (max.toFloat() * level / 100f).toInt().coerceIn(1, max)
            val current = am.getStreamVolume(AudioManager.STREAM_ALARM)
            if (current < target) {
                am.setStreamVolume(AudioManager.STREAM_ALARM, target, 0)
                prevAlarmVolume = current
                boostApplied = true
            }
            audioManager = am
        } catch (_: Exception) {
        }
    }

    private fun restoreBoost() {
        if (boostApplied && audioManager != null && prevAlarmVolume >= 0) {
            try {
                val max = audioManager!!.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                audioManager!!.setStreamVolume(
                    AudioManager.STREAM_ALARM,
                    prevAlarmVolume.coerceAtMost(max),
                    0
                )
            } catch (_: Exception) {
            }
        }
        boostApplied = false
        prevAlarmVolume = -1
        audioManager = null
    }
}