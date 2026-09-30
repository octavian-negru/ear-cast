package app.earcast.ui.background

import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import app.earcast.R

internal data class BatteryStatus(
    val optimizationIgnored: Boolean,
    val backgroundRestricted: Boolean,
)

internal fun Context.readBatteryStatus(): BatteryStatus =
    BatteryStatus(
        optimizationIgnored =
            getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(packageName) == true,
        backgroundRestricted =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                getSystemService(ActivityManager::class.java)?.isBackgroundRestricted == true,
    )

/** Public Settings intents only: manufacturers may omit a destination or enforce extra restrictions. */
internal fun Context.openBatterySettings(appDetails: Boolean = false): Boolean {
    val destinations =
        buildList {
            if (!appDetails) add(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            add(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            add(Intent(Settings.ACTION_SETTINGS))
        }
    val opened = destinations.any { launchSettings(it) }
    if (!opened) Toast.makeText(this, R.string.background_settings_unavailable, Toast.LENGTH_LONG).show()
    return opened
}

private fun Context.launchSettings(intent: Intent): Boolean =
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
