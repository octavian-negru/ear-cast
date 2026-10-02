package app.earcast.assist

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import app.earcast.MainActivity
import app.earcast.data.PreferenceStorage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Quick-settings tile: one-tap Hearing Assist on/off from anywhere. If the mic permission
 * or a usable profile is missing, it opens the app instead of starting. The tile
 * mirrors the shared [LiveAudioController] state, so it stays in sync with the
 * in-app controls.
 */
@AndroidEntryPoint
class LiveAudioTile : TileService() {
    @Inject
    lateinit var controller: LiveAudioController

    @Inject
    lateinit var sessionFactory: LiveSessionBuilder

    @Inject
    lateinit var settingsRepository: PreferenceStorage

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var listenJob: Job? = null

    override fun onStartListening() {
        listenJob =
            scope.launch {
                controller.running
                    .combine(settingsRepository.observeListeningFeatures()) { running, features ->
                        running to features.liveListeningEnabled
                    }.collect { (running, enabled) -> renderTile(running, enabled) }
            }
    }

    override fun onStopListening() {
        listenJob?.cancel()
        listenJob = null
    }

    override fun onClick() {
        if (controller.running.value) {
            LiveAudioService.stop(this)
            return
        }
        val micGranted =
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        if (!micGranted) {
            openApp()
            return
        }
        scope.launch {
            if (sessionFactory.prepare()) LiveAudioService.start(this@LiveAudioTile) else openApp()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun renderTile(
        running: Boolean,
        enabled: Boolean,
    ) {
        val tile = qsTile ?: return
        tile.state =
            when {
                running -> Tile.STATE_ACTIVE
                enabled -> Tile.STATE_INACTIVE
                else -> Tile.STATE_UNAVAILABLE
            }
        tile.updateTile()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openApp() {
        val intent =
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE),
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
