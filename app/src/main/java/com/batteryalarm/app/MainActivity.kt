package com.batteryalarm.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings as AndroidSettings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.mutableStateOf
import com.batteryalarm.app.data.Settings
import com.batteryalarm.app.monitor.AlarmPlayer
import com.batteryalarm.app.monitor.MonitoringService
import com.batteryalarm.app.monitor.Notifications
import com.batteryalarm.app.ui.BatteryAlarmApp
import com.batteryalarm.app.ui.theme.BatteryAlarmTheme

class MainActivity : ComponentActivity() {

    private var pendingStartAfterPermission = false
    private val alarmPending = mutableStateOf(false)
    private val testingAlarm = mutableStateOf(false)
    private val notConnected = mutableStateOf(false)
    private val soundPickVersion = mutableStateOf(0)
    private val themeMode = mutableStateOf(Settings.THEME_SYSTEM)
    private val testHandler = Handler(Looper.getMainLooper())

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            if (pendingStartAfterPermission) {
                pendingStartAfterPermission = false
                MonitoringService.start(this)
            }
        }

    private val pickSoundLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                val resolver = contentResolver
                try {
                    resolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                    // El archivo no admite permisos persistentes; el sonido se usa solo esta vez.
                }
                Settings.setSound(this, uri.toString())
                soundPickVersion.value++
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        themeMode.value = Settings.themeMode(this)
        alarmPending.value = intent?.getBooleanExtra(ALARM_EXTRA, false) == true
        setContent {
            BatteryAlarmTheme(
                darkTheme = when (themeMode.value) {
                    Settings.THEME_DARK -> true
                    Settings.THEME_LIGHT -> false
                    else -> isSystemInDarkTheme()
                }
            ) {
                BatteryAlarmApp(
                    showAlarmDialog = alarmPending.value,
                    showTestAlarmDialog = testingAlarm.value,
                    showNotConnectedDialog = notConnected.value,
                    soundPickVersion = soundPickVersion.value,
                    onStartMonitoring = { ensureNotificationPermissionThenStart() },
                    onStopMonitoring = { MonitoringService.stop(this) },
                    onPickSound = { launchAudioPicker() },
                    onTestAlarm = { startTestAlarm() },
                    onStopTestAlarm = { stopTestAlarm() },
                    onDismissNotConnected = { notConnected.value = false },
                    onThemeChanged = { themeMode.value = Settings.themeMode(this) },
                    onOpenBatterySettings = { openBatterySettings() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(ALARM_EXTRA, false)) {
            alarmPending.value = true
        }
    }

    private fun ensureNotificationPermissionThenStart() {
        if (!MonitoringService.isCharging(this)) {
            notConnected.value = true
            return
        }
        if (Notifications.requestPermissionNeeded(this)) {
            pendingStartAfterPermission = true
            notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            MonitoringService.start(this)
        }
    }

    private fun launchAudioPicker() {
        pickSoundLauncher.launch(arrayOf("audio/*"))
    }

    /**
     * Abre la pantalla para desactivar la optimización de batería de la app.
     * Es lo que permite (en Android 12+ y en muchos celulares) que el monitoreo
     * automático arranque en segundo plano, con la app cerrada.
     */
    private fun openBatterySettings() {
        try {
            val pm = getSystemService(PowerManager::class.java)
            val intent = if (pm?.isIgnoringBatteryOptimizations(packageName) == false) {
                AndroidSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS.let { action ->
                    Intent(action, Uri.parse("package:$packageName"))
                }
            } else {
                Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (_: Exception) {
            // El celular no ofrece esta opción; se ignora silenciosamente.
        }
    }

    /** Reproduce la alarma unos segundos para escucharla antes de conectar el cargador. */
    private fun startTestAlarm() {
        testingAlarm.value = true
        AlarmPlayer.play(this)
        testHandler.removeCallbacksAndMessages(null)
        testHandler.postDelayed({ stopTestAlarm() }, TEST_ALARM_DURATION_MS)
    }

    private fun stopTestAlarm() {
        testHandler.removeCallbacksAndMessages(null)
        AlarmPlayer.stop()
        testingAlarm.value = false
    }

    companion object {
        const val ALARM_EXTRA = "battery_alarm_showing_alarm"
        private const val TEST_ALARM_DURATION_MS = 10000L

        fun alarmIntent(context: android.content.Context): Intent {
            return Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(ALARM_EXTRA, true)
        }
    }
}