package com.batteryalarm.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batteryalarm.app.R
import com.batteryalarm.app.data.Delay
import com.batteryalarm.app.data.Settings
import com.batteryalarm.app.data.Sounds
import com.batteryalarm.app.monitor.Monitor
import com.batteryalarm.app.monitor.MonitorState
import com.batteryalarm.app.util.Units

@Composable
fun BatteryAlarmApp(
    showAlarmDialog: Boolean,
    showTestAlarmDialog: Boolean,
    showNotConnectedDialog: Boolean,
    soundPickVersion: Int,
    onStartMonitoring: () -> Unit,
    onStopMonitoring: () -> Unit,
    onPickSound: () -> Unit,
    onTestAlarm: () -> Unit,
    onStopTestAlarm: () -> Unit,
    onDismissNotConnected: () -> Unit,
    onThemeChanged: () -> Unit,
    onOpenBatterySettings: () -> Unit
) {
    val state by Monitor.state.collectAsState()
    val context = LocalContext.current

    var alarmDialogRemembered by rememberSaveable { mutableStateOf(showAlarmDialog) }

    val showAlarmOverlay = state?.alarmed == true || alarmDialogRemembered

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val active = state?.active == true
            if (active) {
                MonitoringScreen(
                    state = state ?: MonitorState(active = true),
                    onStopMonitoring = onStopMonitoring,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                IdleScreen(
                    onStartMonitoring = onStartMonitoring,
                    onPickSound = onPickSound,
                    onTestAlarm = onTestAlarm,
                    soundPickVersion = soundPickVersion,
                    onThemeChanged = onThemeChanged,
                    onOpenBatterySettings = onOpenBatterySettings,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (showAlarmOverlay) {
                AlarmDialog(
                    state = state,
                    onDismiss = {
                        alarmDialogRemembered = false
                        onStopMonitoring()
                    }
                )
            }

            if (showTestAlarmDialog) {
                TestAlarmDialog(onStop = onStopTestAlarm)
            }

            if (showNotConnectedDialog) {
                NotConnectedDialog(onDismiss = onDismissNotConnected)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Pantalla en reposo
// ---------------------------------------------------------------------------

@Composable
private fun IdleScreen(
    onStartMonitoring: () -> Unit,
    onPickSound: () -> Unit,
    onTestAlarm: () -> Unit,
    soundPickVersion: Int,
    onThemeChanged: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var delayMinutes by rememberSaveable { mutableStateOf(Settings.delayMinutes(context)) }
    var soundLabel by rememberSaveable(soundPickVersion) {
        mutableStateOf(Sounds.label(context, Settings.sound(context)))
    }
    val repoUrl = context.getString(R.string.app_repo_url)
    val openRepo = {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(repoUrl)))
        } catch (_: Exception) {
        }
    }

    var showDelayMenu by remember { mutableStateOf(false) }
    var showCustomDelayDialog by remember { mutableStateOf(false) }
    var showSoundMenu by remember { mutableStateOf(false) }
    var autoStart by remember { mutableStateOf(Settings.autoStart(context)) }
    var boostLevel by remember { mutableStateOf(Settings.boostLevel(context)) }
    var targetLevel by rememberSaveable { mutableStateOf(Settings.targetLevel(context)) }
    var themeMode by rememberSaveable { mutableStateOf(Settings.themeMode(context)) }
    var showThemeMenu by remember { mutableStateOf(false) }

    val selectTheme: (String) -> Unit = { mode ->
        themeMode = mode
        showThemeMenu = false
        Settings.setThemeMode(context, mode)
        onThemeChanged()
    }

    val themeLabel: String = when (themeMode) {
        Settings.THEME_DARK -> stringResource(R.string.theme_dark)
        Settings.THEME_LIGHT -> stringResource(R.string.theme_light)
        else -> stringResource(R.string.theme_system)
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = null,
            modifier = Modifier.size(110.dp)
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onStartMonitoring,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_bolt),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.action_start),
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.hint_start),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(28.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Column {
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDelayMenu = true }
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_timer),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.setting_delay_title),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = stringResource(R.string.setting_delay_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = stringResource(R.string.delay_value, delayMinutes),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    DropdownMenu(
                        expanded = showDelayMenu,
                        onDismissRequest = { showDelayMenu = false }
                    ) {
                        Delay.PRESETS.forEach { minutes ->
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delay_value, minutes)) },
                                onClick = {
                                    delayMinutes = minutes
                                    Settings.setDelayMinutes(context, minutes)
                                    showDelayMenu = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delay_custom)) },
                            onClick = {
                                showDelayMenu = false
                                showCustomDelayDialog = true
                            }
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_battery_charging),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.setting_target_title),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = stringResource(R.string.setting_target_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = stringResource(R.string.setting_target_value, targetLevel),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = targetLevel.toFloat(),
                    onValueChange = { new ->
                        val level = new.toInt().coerceIn(1, 100)
                        targetLevel = level
                        Settings.setTargetLevel(context, level)
                    },
                    valueRange = 1f..100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                )
                Spacer(Modifier.height(8.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSoundMenu = true }
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_sound),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.setting_sound_title),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = soundLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.setting_sound_tip),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = stringResource(R.string.setting_change),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    DropdownMenu(
                        expanded = showSoundMenu,
                        onDismissRequest = { showSoundMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sound_system_alarm)) },
                            onClick = {
                                Settings.setSound(context, Settings.SOUND_SYSTEM_ALARM)
                                soundLabel = Sounds.label(context, Settings.SOUND_SYSTEM_ALARM)
                                showSoundMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sound_system_notification)) },
                            onClick = {
                                Settings.setSound(context, Settings.SOUND_SYSTEM_NOTIFICATION)
                                soundLabel = Sounds.label(context, Settings.SOUND_SYSTEM_NOTIFICATION)
                                showSoundMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sound_pick_file)) },
                            onClick = {
                                showSoundMenu = false
                                onPickSound()
                            }
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            autoStart = !autoStart
                            Settings.setAutoStart(context, autoStart)
                        }
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_bolt),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.setting_autostart_title),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = stringResource(R.string.setting_autostart_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.setting_autostart_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoStart,
                        onCheckedChange = null
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showThemeMenu = true }
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ThemeIcon(
                            mode = themeMode,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.setting_theme_title),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = stringResource(R.string.setting_theme_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = themeLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    DropdownMenu(
                        expanded = showThemeMenu,
                        onDismissRequest = { showThemeMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.theme_system)) },
                            onClick = { selectTheme(Settings.THEME_SYSTEM) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.theme_light)) },
                            onClick = { selectTheme(Settings.THEME_LIGHT) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.theme_dark)) },
                            onClick = { selectTheme(Settings.THEME_DARK) }
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenBatterySettings() }
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_battery_charging),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.setting_background_title),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = stringResource(R.string.setting_background_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = stringResource(R.string.setting_background_open),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val enabled = boostLevel > 0
                            boostLevel = if (enabled) 0 else if (boostLevel == 0) 100 else boostLevel
                            Settings.setBoostLevel(context, boostLevel)
                        }
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_sound),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.setting_boost_title),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = stringResource(R.string.setting_boost_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = boostLevel > 0,
                        onCheckedChange = null
                    )
                }

                if (boostLevel > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.setting_boost_slider),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stringResource(R.string.setting_boost_value, boostLevel),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = boostLevel.toFloat(),
                        onValueChange = { new ->
                            val level = new.toInt().coerceIn(0, 100)
                            boostLevel = level
                            Settings.setBoostLevel(context, level)
                        },
                        valueRange = 0f..100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTestAlarm() }
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_bell),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.setting_test_alarm),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = stringResource(R.string.setting_test_alarm_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = stringResource(R.string.action_test),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable { openRepo() }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_github),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.app_github_label),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            text = stringResource(R.string.app_credits_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.app_credits),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))
    }

    if (showCustomDelayDialog) {
        CustomDelayDialog(
            onDismiss = { showCustomDelayDialog = false },
            onConfirm = { minutes ->
                delayMinutes = minutes
                Settings.setDelayMinutes(context, minutes)
                showCustomDelayDialog = false
            }
        )
    }
}

/**
 * Ícono de tema que anima la transición claro/oscuro (gira suavemente y cambia
 * entre sol y luna según el modo activo).
 */
@Composable
private fun ThemeIcon(mode: String, modifier: Modifier = Modifier) {
    val isDarkNow = mode == Settings.THEME_DARK ||
        (mode == Settings.THEME_SYSTEM && isSystemInDarkTheme())
    val rotation by animateFloatAsState(
        targetValue = if (isDarkNow) 360f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "themeIcon"
    )
    val icon = if (isDarkNow) R.drawable.ic_moon else R.drawable.ic_sun
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier.rotate(rotation)
    )
}

/**
 * Tarjeta con la información y el análisis de la carga: duración, conexión,
 * voltaje, corriente, potencia estimada, temperatura y tiempo restante.
 * Cuando un dato no está disponible se muestra "No disponible" (nunca se
 * inventan valores).
 */
@Composable
private fun ChargeDetailsCard(state: MonitorState) {
    val context = LocalContext.current
    val resources = context.resources
    val r = state.reading
    val unavail = context.getString(R.string.charge_unavailable)

    val etaText = when {
        !state.charging -> unavail
        r.estimateMinutes == null -> context.getString(R.string.charge_calculating)
        r.estimateMinutes < 60 ->
            context.getString(R.string.charge_min, r.estimateMinutes)
        else ->
            context.getString(
                R.string.charge_hour_min,
                r.estimateMinutes / 60,
                r.estimateMinutes % 60
            )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                text = stringResource(R.string.monitor_details_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            StatusRow(
                label = stringResource(R.string.monitor_duration),
                value = Units.formatDuration(state.elapsedSeconds)
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(vertical = 10.dp)
            )
            StatusRow(
                label = stringResource(R.string.monitor_plugged),
                value = r.plugLabel(resources) ?: unavail
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(vertical = 10.dp)
            )
            StatusRow(
                label = stringResource(R.string.monitor_voltage),
                value = r.voltageText()?.let { context.getString(R.string.charge_voltage_v, it) }
                    ?: unavail
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(vertical = 10.dp)
            )
            StatusRow(
                label = stringResource(R.string.monitor_current),
                value = r.currentText()?.let { context.getString(R.string.charge_current_a, it) }
                    ?: unavail
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(vertical = 10.dp)
            )
            StatusRow(
                label = stringResource(R.string.monitor_power),
                value = r.powerText()?.let { context.getString(R.string.charge_power_w, it) }
                    ?: unavail
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(vertical = 10.dp)
            )
            StatusRow(
                label = stringResource(R.string.monitor_temp),
                value = r.tempText()?.let { context.getString(R.string.charge_temp_c, it) }
                    ?: unavail
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(vertical = 10.dp)
            )
            StatusRow(
                label = stringResource(R.string.monitor_eta),
                value = etaText
            )
        }
    }
}

@Composable
private fun CustomDelayDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var text by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delay_custom)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { input -> text = input.filter { it.isDigit() }.take(3) },
                label = { Text(stringResource(R.string.custom_minutes_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    focusManager.clearFocus()
                    val minutes = text.toIntOrNull()
                    if (minutes != null && minutes in 1..120) {
                        onConfirm(minutes)
                    } else {
                        onDismiss()
                    }
                }
            ) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

// ---------------------------------------------------------------------------
// Pantalla de monitoreo activo
// ---------------------------------------------------------------------------

@Composable
private fun MonitoringScreen(
    state: MonitorState,
    onStopMonitoring: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        MonitorStatusHeader(state)

        if (!state.charging && !state.waiting && !state.alarmed) {
            Spacer(Modifier.height(20.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_bolt),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.monitor_connect_prompt),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.monitor_connect_detail),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(40.dp))

        Text(
            text = if (state.waiting || state.alarmed) "${state.targetLevel}%" else "${state.level}%",
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            color = if (state.alarmed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = statusLine(state),
            style = MaterialTheme.typography.titleMedium,
            color = if (state.alarmed)
                MaterialTheme.colorScheme.tertiary
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(20.dp))

        BatteryBar(
            level = state.level,
            target = state.targetLevel,
            charging = state.charging,
            waiting = state.waiting,
            alarmed = state.alarmed,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(28.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Column(Modifier.padding(18.dp)) {
                StatusRow(
                    label = stringResource(R.string.monitor_battery),
                    value = "${state.level}%"
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
                StatusRow(
                    label = stringResource(R.string.monitor_status),
                    value = statusLine(state)
                )
                if (state.waiting && !state.alarmed) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                    StatusRow(
                        label = stringResource(R.string.monitor_alarm_in),
                        value = Units.formatRemaining(state.remainingSeconds)
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        ChargeDetailsCard(state)

        Spacer(Modifier.height(28.dp))

        OutlinedButton(
            onClick = onStopMonitoring,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_stop),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.action_stop), style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.hint_stop),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MonitorStatusHeader(state: MonitorState) {
    val dot = when {
        state.alarmed -> MaterialTheme.colorScheme.tertiary
        state.waiting || state.charging -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val label = when {
        state.alarmed -> stringResource(R.string.state_complete)
        state.waiting || state.charging -> stringResource(R.string.state_charging)
        else -> stringResource(R.string.state_waiting_plug)
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = dot.copy(alpha = 0.15f),
        contentColor = dot
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(dot, RoundedCornerShape(50))
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            if (state.charging && !state.alarmed) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_bolt),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun BatteryBar(
    level: Int,
    target: Int,
    charging: Boolean,
    waiting: Boolean,
    alarmed: Boolean,
    modifier: Modifier = Modifier
) {
    val shownLevel = if (waiting || alarmed) target else level.coerceIn(0, 100)
    val fillColor = when {
        alarmed -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = modifier
            .height(22.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(11.dp))
    ) {
        Row(Modifier.fillMaxSize()) {
            Segment(fillColor, Modifier.weight(1f), active = shownLevel >= 67)
            Spacer(Modifier.width(4.dp))
            Segment(fillColor, Modifier.weight(1f), active = shownLevel in 34..66)
            Spacer(Modifier.width(4.dp))
            Segment(fillColor, Modifier.weight(1f), active = shownLevel <= 33)
        }
        if (charging && !alarmed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_bolt),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
private fun Segment(color: Color, modifier: Modifier, active: Boolean) {
    Box(
        modifier = modifier
            .padding(vertical = 5.dp)
            .background(
                if (active) color else Color.Transparent,
                RoundedCornerShape(6.dp)
            )
    )
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun statusLine(state: MonitorState): String {
    return when {
        state.alarmed -> stringResource(R.string.state_complete_detail)
        state.waiting -> stringResource(
            R.string.state_waiting_detail,
            state.targetLevel,
            Units.formatRemaining(state.remainingSeconds)
        )
        state.charging -> stringResource(R.string.state_charging_detail)
        else -> stringResource(R.string.state_waiting_plug)
    }
}

// ---------------------------------------------------------------------------
// Diálogo de alarma
// ---------------------------------------------------------------------------

@Composable
private fun AlarmDialog(
    state: MonitorState?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val delayMin = state?.delayMinutes ?: Settings.delayMinutes(context)
    val target = state?.targetLevel ?: Settings.targetLevel(context)
    val minUnit = context.getString(if (delayMin == 1) R.string.minuto else R.string.minutos)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.alarm_title),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = stringResource(R.string.alarm_body, delayMin, minUnit, target),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.action_stop_alarm)) }
        }
    )
}

// ---------------------------------------------------------------------------
// Diálogo de probar alarma
// ---------------------------------------------------------------------------

@Composable
private fun TestAlarmDialog(onStop: () -> Unit) {
    AlertDialog(
        onDismissRequest = onStop,
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_bell),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.test_alarm_title),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = stringResource(R.string.test_alarm_body),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(
                onClick = onStop,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.action_stop_test)) }
        }
    )
}

// ---------------------------------------------------------------------------
// Diálogo "no estás conectado"
// ---------------------------------------------------------------------------

@Composable
private fun NotConnectedDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_bolt),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.not_connected_title),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = stringResource(R.string.not_connected_body),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.action_ok)) }
        }
    )
}