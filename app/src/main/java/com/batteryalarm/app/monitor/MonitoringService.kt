package com.batteryalarm.app.monitor

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.batteryalarm.app.data.Settings

/**
 * Servicio en primer plano que vigila la carga mientras el usuario lo decide.
 *
 * Flujo: el usuario pulsa "Iniciar monitoreo" → este servicio se mantiene
 * activo en la barra de estado. Cuando la batería llega al nivel configurado
 * (o al 100% por defecto) espera el tiempo configurado y entonces suena la
 * alarma. El servicio termina cuando el usuario lo detiene (o al desconectar
 * el cargador antes del nivel).
 */
class MonitoringService : Service() {

    companion object {
        private const val TAG = "BatteryAlarm"

        const val ACTION_START = "com.batteryalarm.app.action.START"
        const val ACTION_STOP = "com.batteryalarm.app.action.STOP"

        fun start(context: Context) {
            val intent = Intent(context, MonitoringService::class.java)
                .setAction(ACTION_START)
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MonitoringService::class.java))
        }

        /** Lee la última emisión (sticky) y dice si el cargador está conectado. */
        fun isCharging(context: Context): Boolean {
            return try {
                val intent = context.registerReceiver(
                    null,
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                )
                val status = intent?.getIntExtra(
                    BatteryManager.EXTRA_STATUS,
                    BatteryManager.BATTERY_STATUS_UNKNOWN
                ) ?: BatteryManager.BATTERY_STATUS_UNKNOWN
                status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            } catch (_: Exception) {
                false
            }
        }

        fun stopIntent(context: Context): PendingIntent {
            val intent = Intent(context, MonitoringService::class.java)
                .setAction(ACTION_STOP)
            return PendingIntent.getService(
                context,
                100,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var receiverRegistered = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                onBatteryChanged(intent)
            }
        }
    }

    private val ticker = object : Runnable {
        override fun run() {
            val state = Monitor.state.value ?: return
            if (state.waiting && state.alarmed) return

            val remaining = state.remainingSeconds - 1
            if (remaining <= 0) {
                fireAlarm()
            } else {
                Monitor.update { it.copy(remainingSeconds = remaining) }
                val current = Monitor.state.value ?: return
                Notifications.showMonitoring(this@MonitoringService, current)
                mainHandler.postDelayed(this, 1000L)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannels(this)
        registerBatteryReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                Log.i(TAG, "Monitoreo detenido por el usuario")
                stopSelf()
            }
            else -> startMonitoring()
        }
        return START_NOT_STICKY
    }

    private fun startMonitoring() {
        if (!receiverRegistered) registerBatteryReceiver()

        // Estado inicial a partir de la emisión más reciente (sticky).
        val current = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (current == null) {
            Log.w(TAG, "No se pudo leer el estado inicial de la batería")
            Monitor.reset()
            stopSelf()
            return
        }
        syncState(current)
        if (Monitor.state.value?.charging != true) {
            // El cargador acaba de conectarse y la emisión sticky (battery) puede
            // todavía decir "no cargando" por unos instantes. No nos detenemos aquí:
            // el receiver detectará la carga en cuanto llegue.
            Log.i(TAG, "Se espera a que la carga comience")
        } else {
            processBatteryLogic(current)
        }

        val state = Monitor.state.value ?: MonitorState(active = true)
        startForeground(Notifications.ID_MONITORING, Notifications.monitoringNotification(this, state))
    }

    private fun registerBatteryReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(batteryReceiver, filter)
        }
        receiverRegistered = true
    }

    private fun onBatteryChanged(intent: Intent) {
        syncState(intent)
        processBatteryLogic(intent)
    }

    private fun processBatteryLogic(intent: Intent) {
        val state = Monitor.state.value ?: return
        val level = state.level
        val charging = state.charging

        if (!charging) {
            // El cargador se desconectó: se cancela el monitoreo.
            Log.i(TAG, "Cargador desconectado, se detiene el monitoreo (nivel $level%)")
            stopSelf()
            return
        }

        if (level >= state.targetLevel && !state.waiting) {
            startWaiting()
        } else if (level < state.targetLevel && !state.alarmed) {
            Monitor.update { it.copy(waiting = false, remainingSeconds = 0) }
        }
    }

    private fun syncState(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val percent = if (scale > 0) (level * 100) / scale else 0
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        Monitor.update {
            it.copy(
                charging = charging,
                level = percent,
                delayMinutes = Settings.delayMinutes(this),
                targetLevel = Settings.targetLevel(this)
            )
        }
        Notifications.showMonitoring(this, Monitor.state.value ?: return)
    }

    private fun startWaiting() {
        val delayMin = Settings.delayMinutes(this)
        Monitor.update {
            it.copy(
                waiting = true,
                remainingSeconds = delayMin * 60,
                alarmed = false,
                delayMinutes = delayMin
            )
        }
        mainHandler.removeCallbacks(ticker)
        mainHandler.postDelayed(ticker, 1000L)
    }

    private fun fireAlarm() {
        val state = Monitor.state.value ?: return
        val delayMin = state.delayMinutes
        Monitor.update { it.copy(remainingSeconds = 0, alarmed = true, waiting = true) }
        val current = Monitor.state.value ?: return
        Notifications.showAlarm(this, current)
        Notifications.showMonitoring(this, current)
        AlarmPlayer.play(this)
        Log.i(TAG, "¡Nivel ${current.targetLevel}% alcanzado! Alarma sonando después de $delayMin minuto(s)")
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        try {
            if (receiverRegistered) {
                unregisterReceiver(batteryReceiver)
                receiverRegistered = false
            }
        } catch (_: Exception) {
        }
        AlarmPlayer.stop()
        Notifications.cancelMonitoring(this)
        Notifications.cancelAlarm(this)
        Monitor.reset()
        Log.i(TAG, "Servicio de monitoreo terminado")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}