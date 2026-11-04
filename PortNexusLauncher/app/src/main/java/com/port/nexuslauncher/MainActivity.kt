package com.port.nexuslauncher

import android.content.ComponentName
import android.content.Intent
import android.os.BatteryManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/** Apenas aplicativos com uma Activity LAUNCHER visível para este perfil. */
data class LauncherApp(
    val label: String,
    val component: ComponentName,
) {
    val packageName: String get() = component.packageName
}

enum class NexusAction {
    WIFI_SETTINGS, BLUETOOTH_SETTINGS, ANDROID_SETTINGS, HOME_SETTINGS, CLOCK, SPOTIFY, DISPLAY_SETTINGS, SOUND_SETTINGS, CALENDAR, STORAGE_SETTINGS
}

class MainActivity : ComponentActivity() {
    private var installedApps by mutableStateOf<List<LauncherApp>>(emptyList())
    private var batteryPercent by mutableIntStateOf(-1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(9, 6, 11)
        window.navigationBarColor = android.graphics.Color.rgb(9, 6, 11)
        refreshInstalledApps()
        batteryPercent = readBatteryPercent()

        setContent {
            PortNexusV2(
                apps = installedApps,
                prefs = getSharedPreferences("port_nexus_settings", MODE_PRIVATE),
                batteryPercent = batteryPercent,
                onLaunchApp = ::launchApp,
                onSystemAction = ::performAction,
                onReloadApps = ::refreshInstalledApps,
                onBrightnessChange = ::setLauncherBrightness,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        refreshInstalledApps()
        batteryPercent = readBatteryPercent()
    }

    @Suppress("DEPRECATION")
    private fun refreshInstalledApps() {
        val launcherQuery = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        installedApps = packageManager.queryIntentActivities(launcherQuery, 0)
            .mapNotNull { info ->
                val activity = info.activityInfo ?: return@mapNotNull null
                if (!activity.enabled) return@mapNotNull null
                LauncherApp(
                    label = info.loadLabel(packageManager).toString(),
                    component = ComponentName(activity.packageName, activity.name),
                )
            }
            .distinctBy { it.component.flattenToString() }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }

    private fun launchApp(app: LauncherApp) {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(app.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        safeStart(intent)
    }

    private fun readBatteryPercent(): Int {
        val manager = getSystemService(BATTERY_SERVICE) as BatteryManager
        return manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun setLauncherBrightness(level: Float) {
        val params = window.attributes
        params.screenBrightness = level.coerceIn(0.05f, 1f)
        window.attributes = params
    }

    private fun performAction(action: NexusAction) {
        val intent = when (action) {
            NexusAction.WIFI_SETTINGS -> Intent(Settings.ACTION_WIFI_SETTINGS)
            NexusAction.BLUETOOTH_SETTINGS -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            NexusAction.ANDROID_SETTINGS -> Intent(Settings.ACTION_SETTINGS)
            NexusAction.HOME_SETTINGS -> Intent(Settings.ACTION_HOME_SETTINGS)
            NexusAction.CLOCK -> Intent("android.intent.action.SHOW_ALARMS")
            NexusAction.DISPLAY_SETTINGS -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
            NexusAction.SOUND_SETTINGS -> Intent(Settings.ACTION_SOUND_SETTINGS)
            NexusAction.CALENDAR -> Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR)
            NexusAction.STORAGE_SETTINGS -> Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
            NexusAction.SPOTIFY -> packageManager.getLaunchIntentForPackage("com.spotify.music")
                ?: Intent("android.intent.action.MUSIC_PLAYER")
        }
        safeStart(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun safeStart(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "Nenhum aplicativo disponível para esta ação.", Toast.LENGTH_SHORT).show()
        }
    }
}
