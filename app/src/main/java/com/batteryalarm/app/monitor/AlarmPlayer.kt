package com.batteryalarm.app.monitor

import android.content.Context
import android.media.MediaPlayer
import android.os.VibrationEffect
import android.os.Vibrator
import com.batteryalarm.app.data.Settings
import com.batteryalarm.app.data.Sounds

/** Reproduce la alarma de forma independiente hasta que se detenga. */
object AlarmPlayer {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    @Synchronized
    fun play(context: Context) {
        stop()

        val key = Settings.sound(context)
        val uri = Sounds.resolveUri(context, key)
        if (uri != null) {
            try {
                val p = MediaPlayer()
                p.setDataSource(context, uri)
                p.setOnPreparedListener {
                    it.isLooping = true
                    it.start()
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
            vibrator?.cancel()
        } catch (_: Exception) {
        }
        vibrator = null
    }
}