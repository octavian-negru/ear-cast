@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.background

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.earcast.R
import app.earcast.ui.common.SectionGroup

/** A one-time suggestion after safety consent; dismissing never disables an app feature. */
@Composable
fun BackgroundSetupPrompt(
    reviewed: Boolean?,
    onReviewed: () -> Unit,
) {
    val context = LocalContext.current
    val status = rememberBatteryStatus()
    val alreadyExempt = status.optimizationIgnored && !status.backgroundRestricted
    LaunchedEffect(reviewed, alreadyExempt) {
        if (reviewed == false && alreadyExempt) onReviewed()
    }
    if (reviewed != false || alreadyExempt) return
    AlertDialog(
        onDismissRequest = onReviewed,
        title = { Text(stringResource(R.string.background_listening_title)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.background_listening_explanation))
                Text(stringResource(R.string.background_battery_instructions))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (context.openBatterySettings(appDetails = status.backgroundRestricted)) onReviewed()
            }) {
                Text(stringResource(R.string.background_open_battery_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onReviewed) { Text(stringResource(R.string.background_not_now)) }
        },
    )
}

@Composable
fun BackgroundListeningSettings() {
    val context = LocalContext.current
    val status = rememberBatteryStatus()
    val statusText =
        when {
            status.backgroundRestricted -> R.string.background_status_restricted
            status.optimizationIgnored -> R.string.background_status_exempt
            else -> R.string.background_status_optimized
        }
    SectionGroup(stringResource(R.string.background_listening_title)) {
        Text(stringResource(statusText), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.background_listening_explanation), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.background_battery_instructions), style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = { context.openBatterySettings() }) {
            Text(stringResource(R.string.background_open_battery_settings))
        }
        TextButton(onClick = { context.openBatterySettings(appDetails = true) }) {
            Text(stringResource(R.string.background_open_app_settings))
        }
    }
}

/** Read the OS state again on return from Settings instead of remembering a supposed permission grant. */
@Composable
private fun rememberBatteryStatus(): BatteryStatus {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var status by remember(context) { mutableStateOf(context.readBatteryStatus()) }
    DisposableEffect(context, lifecycle) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) status = context.readBatteryStatus()
            }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return status
}
