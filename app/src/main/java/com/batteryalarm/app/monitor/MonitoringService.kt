package com.batteryalarm.app.monitor

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.batteryalarm.app.data.ChargeEstimator
import com.batteryalarm.app.data.ChargeHistory
import com.batteryalarm.app.data.ChargePoint
import com.batteryalarm.app.data.ChargeReading
import com.batteryalarm.app.data.Settings

/**
 * Servicio en primer plano que vigila la carga mientras el usuario lo decide.
 *
 * Flujo: el usuario pulsa "Iniciar monitoreo" → este servicio se mantiene
 * activo en la barra de estado. Cuando la batería llega al nivel configurado
 * (o al 100% por defecto) espera el tiempo configurado y entonces suena la
 * alarma. El servicio termina cuando el usuario lo detiene (o al desconectar
 * el cargador antes del nivel).
 *
 * Mientras está activo también recopila información útil de la carga (voltaje,
 * corriente, potencia estimada, temperatura y tiempo estimado al 100%) que se
 * muestra en la notificación persistente y en la pantalla de monitoreo. Al
 * finalizar la sesión, el resumen se guarda localmente (ChargeHistory) para
 * un futuro historial. El servicio solo existe mientras el monitoreo está en
 * marcha: no monitorea la batería de forma permanente.
 */
class MonitoringService : Service() {

    companion object {
        private const val TAG = "BatteryAlarm"

        const val ACTION_START = "com.batteryalarm.app.action.START"
        const val ACTION_STOP = "com.batteryalarm.app.action.STOP"

        @Volatile
        private var running = false

        fun start(context: Context) {
            if (running) return
            val intent = Intent(context, MonitoringService::class.java)
                .setAction(ACTION_START)
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }

        /**
         * Inicia el monitoreo desde el receiver de POWER_CONNECTED.
         *
         * En Android 12+ el sistema puede bloquear el arranque de servicios en
         * primer plano desde segundo plano (ForegroundServiceStartNotAllowedException).
         * En ese caso no se puede forzar; se avisa al usuario con una notificación
         * que permite iniciar el monitoreo con un toque, que es el caso permitido
         * por el sistema.
         */
        fun startFromBackground(context: Context) {
            if (running) return
            try {
                start(context)
            } catch (t: Throwable) {
                Log.w(TAG, "Arranque en segundo plano bloqueado; se notifica al usuario: $t")
                Notifications.showAutoStartNotice(context)
            }
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

        /** Intent para arrancar (usado por la notificación de aviso en segundo plano). */
        fun startIntent(context: Context): PendingIntent {
            val intent = Intent(context, MonitoringService::class.java)
                .setAction(ACTION_START)
            return PendingIntent.getService(
                context,
                101,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var receiverRegistered = false

    // Datos de la sesión de carga en curso.
    private var sessionStart = 0L
    private var sessionStartLevel = 0
    private val chargePoints = mutableListOf<ChargePoint>()
    private var powerSum = 0.0
    private var powerCount = 0
    private var maxPower: Double? = null
    private var maxTemp: Int? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                onBatteryChanged(intent)
            }
        }
    }

    private val counter = object : Runnable {
        override fun run() {
            val state = Monitor.state.value ?: return
            if (state.alarmed) return

            if (state.waiting) {
                val remaining = state.remainingSeconds - 1
                if (remaining <= 0) {
                    fireAlarm()
                    return
                }
                Monitor.update {
                    it.copy(remainingSeconds = remaining, elapsedSeconds = it.elapsedSeconds + 1)
                }
                val current = Monitor.state.value ?: return
                Notifications.showMonitoring(this@MonitoringService, current)
            } else if (state.charging) {
                // Solo actualiza el tiempo transcurrido; la notificación se refresca
                // cuando cambia la batería (eventos), no cada segundo.
                Monitor.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
            mainHandler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        running = true
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
            beginSession()
            processBatteryLogic(current)
        }

        val state = Monitor.state.value ?: MonitorState(active = true)
        mainHandler.removeCallbacks(counter)
        mainHandler.postDelayed(counter, 1000L)
        startForeground(Notifications.ID_MONITORING, Notifications.monitoringNotification(this, state))
    }

    private fun registerBatteryReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
        val status = intent.getIntExtra(
            BatteryManager.EXTRA_STATUS,
            BatteryManager.BATTERY_STATUS_UNKNOWN
        )
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        // Corrige el desfase común de "100% en la barra pero 99% en la app":
        // cuando el sistema ya marcó la batería como FULL, se reporta 100.
        val percent = when {
            status == BatteryManager.BATTERY_STATUS_FULL -> 100
            scale > 0 -> (level * 100) / scale
            else -> 0
        }.coerceIn(0, 100)

        val now = System.currentTimeMillis()
        val reading = readChargeInfo(intent, charging)
        val estimate = if (charging) {
            ChargeEstimator.estimateMinutes(chargePoints, percent, now)
        } else {
            null
        }

        if (charging) {
            if (sessionStart <= 0L) beginSession()
            recordPoint(percent, now)
            reading.powerWatts?.let { p ->
                powerSum += p
                powerCount++
                if (maxPower == null || p > maxPower!!) maxPower = p
            }
            reading.temperatureTenths?.takeIf { it != 0 }?.let { t ->
                if (maxTemp == null || t > maxTemp!!) maxTemp = t
            }
        }

        Monitor.update {
            it.copy(
                charging = charging,
                level = percent,
                delayMinutes = Settings.delayMinutes(this),
                targetLevel = Settings.targetLevel(this),
                reading = reading.copy(estimateMinutes = estimate)
            )
        }
        Notifications.showMonitoring(this, Monitor.state.value ?: return)
    }

    /**
     * Lee los datos de la carga disponibles. Todos son opcionales: si un
     * teléfono no los reporta quedan en `null` y la UI/notificación muestran
     * "No disponible" u ocultan la línea. Nunca se inventan valores.
     */
    private fun readChargeInfo(intent: Intent, charging: Boolean): ChargeReading {
        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0).takeIf { it > 0 }
        val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0).takeIf { it != 0 }
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)

        var current: Int? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val bm = getSystemService(BatteryManager::class.java)
                val c = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                if (c != null && c > 0) current = c
            } catch (_: Exception) {
                // Dispositivo sin soporte: corriente no disponible.
            }
        }

        return ChargeReading(
            pluggedType = plugged,
            voltageMv = voltage,
            currentMicroA = current,
            temperatureTenths = temp
        )
    }

    private fun beginSession() {
        if (sessionStart > 0L) return
        sessionStart = System.currentTimeMillis()
        sessionStartLevel = Monitor.state.value?.level ?: 0
        chargePoints.clear()
        powerSum = 0.0
        powerCount = 0
        maxPower = null
        maxTemp = null
        Monitor.update { it.copy(elapsedSeconds = 0) }
    }

    private fun recordPoint(level: Int, now: Long) {
        if (chargePoints.isNotEmpty()) {
            val last = chargePoints.last()
            if (now - last.ts < 20_000L && last.level == level) return
        }
        val elapsed = if (sessionStart > 0L) ((now - sessionStart) / 1000L).coerceAtLeast(0L) else 0L
        chargePoints.add(ChargePoint(level = level, ts = now, elapsedSec = elapsed))
    }

    private fun finalizeSession() {
        if (sessionStart <= 0L) return
        val state = Monitor.state.value ?: MonitorState(active = true)
        val endTime = System.currentTimeMillis()
        val avgPower = if (powerCount > 0) powerSum / powerCount else null
        val reached = (state.level >= state.targetLevel) && (state.waiting || state.alarmed)

        val session = com.batteryalarm.app.data.ChargeSession(
            startTime = sessionStart,
            startLevel = sessionStartLevel,
            endTime = endTime,
            endLevel = state.level,
            reachedTarget = reached,
            avgPowerW = avgPower,
            maxPowerW = maxPower,
            maxTempTenths = maxTemp,
            points = chargePoints.toList()
        )

        val durationMs = endTime - sessionStart
        if (chargePoints.size >= 2 && durationMs >= 60_000L) {
            ChargeHistory.save(this, session)
        }
        sessionStart = 0L
        chargePoints.clear()
        powerSum = 0.0
        powerCount = 0
        maxPower = null
        maxTemp = null
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
        finalizeSession()
        running = false
        AlarmPlayer.stop()
        Notifications.cancelMonitoring(this)
        Notifications.cancelAlarm(this)
        Monitor.reset()
        Log.i(TAG, "Servicio de monitoreo terminado")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}