package com.batteryalarm.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        alarmPending.value = intent?.getBooleanExtra(ALARM_EXTRA, false) == true
        setContent {
            BatteryAlarmTheme {
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
                    onDismissNotConnected = { notConnected.value = false }
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