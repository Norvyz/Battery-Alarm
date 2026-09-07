package com.batteryalarm.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import com.batteryalarm.app.data.Settings
import com.batteryalarm.app.monitor.MonitoringService
import com.batteryalarm.app.monitor.Notifications
import com.batteryalarm.app.ui.BatteryAlarmApp
import com.batteryalarm.app.ui.theme.BatteryAlarmTheme

class MainActivity : ComponentActivity() {

    private var pendingStartAfterPermission = false
    private val alarmPending = mutableStateOf(false)

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
                    onStartMonitoring = { ensureNotificationPermissionThenStart() },
                    onStopMonitoring = { MonitoringService.stop(this) },
                    onPickSound = { launchAudioPicker() }
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

    companion object {
        const val ALARM_EXTRA = "battery_alarm_showing_alarm"

        fun alarmIntent(context: android.content.Context): Intent {
            return Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(ALARM_EXTRA, true)
        }
    }
}