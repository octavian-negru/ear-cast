package app.openhearing.core.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/** One session's focus and device selection. All changes are released on every exit path. */
@Suppress("DEPRECATION")
internal class AssistAudioRoute(
    private val context: Context,
    private val onLost: () -> Unit,
) : AutoCloseable {
    private val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var previousMode: Int? = null
    private var communicationRequested = false
    private var scoRequested = false
    private var receiverRegistered = false
    private var focus: AudioFocusRequest? = null

    @Volatile private var scoConnected = false

    @Volatile private var hadScoConnection = false

    private val scoReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                scoConnected = intent?.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1) ==
                    AudioManager.SCO_AUDIO_STATE_CONNECTED
                if (scoConnected) {
                    hadScoConnection = true
                } else if (hadScoConnection) {
                    onLost()
                }
            }
        }

    lateinit var input: AudioDeviceInfo
        private set
    var output: AudioDeviceInfo? = null
        private set
    var communication = false
        private set
    val sco: Boolean get() = output?.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO

    val attributes: AudioAttributes
        get() =
            AudioAttributes
                .Builder()
                .setUsage(if (communication) AudioAttributes.USAGE_VOICE_COMMUNICATION else AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

    fun open(
        source: MicrophoneSource,
        requireHeadphones: Boolean,
        keepRunning: () -> Boolean,
    ) {
        check(manager.mode == AudioManager.MODE_NORMAL) { "Another call or audio session is using the microphone." }
        if (source == MicrophoneSource.HEADSET) {
            selectHeadset(keepRunning)
        } else {
            input = manager
                .getDevices(AudioManager.GET_DEVICES_INPUTS)
                .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }
                ?: error("The phone microphone is unavailable.")
            val outputs = manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            output = PHONE_OUTPUT_TYPES.firstNotNullOfOrNull { type -> outputs.firstOrNull { it.type == type } }
        }
        check(!requireHeadphones || output != null) { "Connect headphones before starting Hearing Assist." }
        val request =
            AudioFocusRequest
                .Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener({ change ->
                    if (change < AudioManager.AUDIOFOCUS_GAIN) onLost()
                }, Handler(Looper.getMainLooper()))
                .build()
        focus = request
        check(manager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            "Audio is in use by another app. Stop it and try again."
        }
    }

    private fun selectHeadset(keepRunning: () -> Boolean) {
        val devices = manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
        val candidates =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (manager.availableCommunicationDevices + devices).distinctBy { it.id }
            } else {
                devices
            }
        val inputs = manager.getDevices(AudioManager.GET_DEVICES_INPUTS)
        val usable =
            candidates.filter { device ->
                device.type in BLUETOOTH_TYPES || matchingInput(inputs, device) != null
            }
        val selected =
            HEADSET_TYPES.firstNotNullOfOrNull { type -> usable.firstOrNull { it.type == type } }
                ?: error("No headset microphone is available. Enable headset call audio or use the phone microphone.")
        output = selected
        communication = selected.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            selected.type == AudioDeviceInfo.TYPE_BLE_HEADSET
        if (communication) {
            previousMode = manager.mode
            manager.mode = AudioManager.MODE_IN_COMMUNICATION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                communicationRequested = true
                check(manager.setCommunicationDevice(selected)) { "Android could not select the headset." }
                awaitRoute(keepRunning) { manager.communicationDevice?.id == selected.id }
            } else {
                check(manager.isBluetoothScoAvailableOffCall) {
                    "This phone does not support headset microphone audio."
                }
                context.registerReceiver(scoReceiver, IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED))
                receiverRegistered = true
                scoRequested = true
                manager.startBluetoothSco()
                manager.isBluetoothScoOn = true
                awaitRoute(keepRunning) { scoConnected }
            }
        }
        // Some devices publish the input endpoint after the SCO/LE connection callback.
        var selectedInput: AudioDeviceInfo? = null
        awaitRoute(keepRunning) {
            selectedInput = matchingInput(manager.getDevices(AudioManager.GET_DEVICES_INPUTS), selected)
            selectedInput != null
        }
        input = checkNotNull(selectedInput)
    }

    private fun awaitRoute(
        keepRunning: () -> Boolean,
        ready: () -> Boolean,
    ) {
        val deadline = SystemClock.elapsedRealtime() + CONNECT_TIMEOUT_MS
        while (!ready()) {
            if (!keepRunning()) throw InterruptedException("Stopped")
            check(SystemClock.elapsedRealtime() < deadline) { "Headset connection timed out. Reconnect and try again." }
            Thread.sleep(POLL_MS)
        }
    }

    fun matches(
        inputDevice: AudioDeviceInfo?,
        outputDevice: AudioDeviceInfo?,
    ): Boolean = inputDevice?.id == input.id && (output?.let { it.id == outputDevice?.id } ?: true)

    override fun close() {
        if (receiverRegistered) runCatching { context.unregisterReceiver(scoReceiver) }
        if (communicationRequested && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { manager.clearCommunicationDevice() }
        }
        if (scoRequested) {
            runCatching { manager.stopBluetoothSco() }
            runCatching { manager.isBluetoothScoOn = false }
        }
        previousMode?.let { mode ->
            runCatching { if (manager.mode == AudioManager.MODE_IN_COMMUNICATION) manager.mode = mode }
        }
        focus?.let { runCatching { manager.abandonAudioFocusRequest(it) } }
    }

    private fun matchingInput(
        inputs: Array<AudioDeviceInfo>,
        selected: AudioDeviceInfo,
    ): AudioDeviceInfo? {
        fun identity(device: AudioDeviceInfo) =
            HeadsetIdentity(
                device.id,
                device.type,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) device.address else "",
                device.productName.toString(),
            )
        val id = matchingHeadsetInput(inputs.map(::identity), identity(selected))
        return inputs.firstOrNull { it.id == id }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 10_000L
        const val POLL_MS = 20L
        val BLUETOOTH_TYPES = setOf(AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
        val HEADSET_TYPES =
            listOf(
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_USB_HEADSET,
                AudioDeviceInfo.TYPE_USB_DEVICE,
                AudioDeviceInfo.TYPE_BLE_HEADSET,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            )

        // Phone input uses media playback; selecting an inactive SCO endpoint here would fail routing.
        val PHONE_OUTPUT_TYPES =
            listOf(
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_USB_HEADSET,
                AudioDeviceInfo.TYPE_USB_DEVICE,
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLE_HEADSET,
                AudioDeviceInfo.TYPE_HEARING_AID,
                AudioDeviceInfo.TYPE_BLE_SPEAKER,
            )
    }
}
