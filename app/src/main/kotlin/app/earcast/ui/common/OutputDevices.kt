package app.earcast.ui.common

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager

private val HEADPHONE_TYPES =
    setOf(
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_HEARING_AID,
        AudioDeviceInfo.TYPE_BLE_HEADSET,
    )

/**
 * True when a personal listening device (wired/BT/USB headset, hearing aid) is
 * the likely output. Assist warns without one because microphone-to-speaker
 * feedback can be unpleasant or loud.
 */
internal fun headphonesConnected(context: Context): Boolean {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { it.type in HEADPHONE_TYPES }
}
