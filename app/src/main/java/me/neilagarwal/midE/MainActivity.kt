package me.neilagarwal.midE

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ActivityCompat
import com.google.android.material.composethemeadapter.MdcTheme
import jp.kshoji.blemidi.central.BleMidiCentralProvider
import jp.kshoji.blemidi.device.MidiInputDevice
import jp.kshoji.blemidi.device.MidiOutputDevice
import jp.kshoji.blemidi.listener.OnMidiDeviceAttachedListener
import jp.kshoji.blemidi.listener.OnMidiDeviceDetachedListener
import jp.kshoji.blemidi.peripheral.BleMidiPeripheralProvider
import jp.kshoji.driver.midi.activity.AbstractSingleMidiActivity

class MainActivity : AbstractSingleMidiActivity() {
    private var bleMidiPeripheralProvider: BleMidiPeripheralProvider? = null
    private var bleMidiCentralProvider: BleMidiCentralProvider? = null
    var midiOutputDevice: MidiOutputDevice? = null

    private val model: MainViewModel by viewModels();

    var advertising = false
    var isBleOutputConnected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main);
        val mainComposable = findViewById<ComposeView>(R.id.main)
        mainComposable.setContent {
            MdcTheme {
                MainComposable()
            }
        }

        checkAndAskForPermissions()

        setupBlePeripheralProvider()
        setupBleCentralProvider()
    }

    private fun checkAndAskForPermissions() {
        val permissions =
            arrayOf(
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_COARSE_LOCATION
            );
        if (!hasPermissions(this, *permissions)) {
            ActivityCompat.requestPermissions(this, permissions, 1);
        }
    }

    private fun hasPermissions(context: Context, vararg permissions: String): Boolean =
        permissions.all {
            ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }


    @Composable
    private fun MainComposable() {
        Column() {
            Text(
                text = "USB In/Out: ${model.usbMidiDeviceName}",
            )
            Text(
                text = "BLE Out: ${model.bleMidiDevicename}",
            )
            Button(onClick = {
                model.testing = bleMidiCentralProvider!!.midiOutputDevices.count().toString()
            }) {
                Text("Get device name")
            }
            Button(onClick = {
                bleMidiCentralProvider!!.startScanDevice(30000)
            }) {
                Text("Scan")
            }
            Text(
                text = "Device name: ${model.testing}"
            )
        }
    }

    private fun setupBlePeripheralProvider() {
        bleMidiPeripheralProvider = BleMidiPeripheralProvider(this)
        bleMidiPeripheralProvider!!.setDeviceName(model.bleMidiDevicename)
        bleMidiPeripheralProvider!!.setManufacturer("midE")

        // Listener for Device connections and disconnections
        bleMidiPeripheralProvider!!.setOnMidiDeviceAttachedListener(object :
            OnMidiDeviceAttachedListener {
            override fun onMidiInputDeviceAttached(midiInputDevice: MidiInputDevice) {}
            override fun onMidiOutputDeviceAttached(localMidiOutputDevice: MidiOutputDevice) {
                midiOutputDevice = localMidiOutputDevice
                isBleOutputConnected = true
                model.bleMidiDevicename = midiOutputDevice!!.deviceName
            }
        })
        bleMidiPeripheralProvider!!.setOnMidiDeviceDetachedListener(object :
            OnMidiDeviceDetachedListener {
            override fun onMidiInputDeviceDetached(midiInputDevice: MidiInputDevice) {}
            override fun onMidiOutputDeviceDetached(localMidiOutputDevice: MidiOutputDevice) {
                isBleOutputConnected = false
            }
        })
    }

    private fun setupBleCentralProvider() {
        bleMidiCentralProvider = BleMidiCentralProvider(this)
        // Listener for Device disconnection
        bleMidiCentralProvider!!.setRequestPairing(true)
        bleMidiCentralProvider!!.setOnMidiDeviceAttachedListener(object :
            OnMidiDeviceAttachedListener {
            override fun onMidiInputDeviceAttached(midiInputDevice: MidiInputDevice) {
                // attach the MIDI Input Listener
//                midiInputDevice.setOnMidiInputEventListener(onMidiInputEventListener)

                // TODO process event
            }

            override fun onMidiOutputDeviceAttached(midiOutputDevice: MidiOutputDevice) {
                // TODO process event
            }
        })

// Listener for Device disconnection

// Listener for Device disconnection
        bleMidiCentralProvider!!.setOnMidiDeviceDetachedListener(object :
            OnMidiDeviceDetachedListener {
            override fun onMidiInputDeviceDetached(midiInputDevice: MidiInputDevice) {
                // TODO process event
            }

            override fun onMidiOutputDeviceDetached(midiOutputDevice: MidiOutputDevice) {
                // TODO process event
            }
        })

    }

    override fun onDeviceAttached(usbDevice: UsbDevice) {}
    override fun onMidiInputDeviceAttached(midiInputDevice: jp.kshoji.driver.midi.device.MidiInputDevice) {}
    override fun onMidiOutputDeviceAttached(midiOutputDevice: jp.kshoji.driver.midi.device.MidiOutputDevice) {
        model.usbMidiDeviceName = midiOutputDevice.productName!!;
    }

    override fun onDeviceDetached(usbDevice: UsbDevice) {}
    override fun onMidiInputDeviceDetached(midiInputDevice: jp.kshoji.driver.midi.device.MidiInputDevice) {}
    override fun onMidiOutputDeviceDetached(midiOutputDevice: jp.kshoji.driver.midi.device.MidiOutputDevice) {
        model.usbMidiDeviceName = model.noDeviceDeviceName;
    }

    override fun onMidiMiscellaneousFunctionCodes(
        sender: jp.kshoji.driver.midi.device.MidiInputDevice,
        cable: Int,
        byte1: Int,
        byte2: Int,
        byte3: Int
    ) {
    }

    override fun onMidiCableEvents(
        sender: jp.kshoji.driver.midi.device.MidiInputDevice,
        cable: Int,
        byte1: Int,
        byte2: Int,
        byte3: Int
    ) {
    }

    override fun onMidiSystemCommonMessage(
        sender: jp.kshoji.driver.midi.device.MidiInputDevice,
        cable: Int,
        bytes: ByteArray
    ) {
    }

    override fun onMidiSingleByte(
        sender: jp.kshoji.driver.midi.device.MidiInputDevice,
        cable: Int,
        byte1: Int
    ) {
    }

    override fun onMidiSystemExclusive(
        sender: jp.kshoji.driver.midi.device.MidiInputDevice,
        cable: Int,
        systemExclusive: ByteArray
    ) {
        if (isBleOutputConnected) midiOutputDevice!!.sendMidiSystemExclusive(systemExclusive)
    }

    override fun onMidiNoteOn(
        sender: jp.kshoji.driver.midi.device.MidiInputDevice,
        cable: Int,
        channel: Int,
        note: Int,
        velocity: Int
    ) {
        if (isBleOutputConnected) midiOutputDevice!!.sendMidiNoteOn(channel, note, velocity)
    }

    override fun onMidiNoteOff(
        sender: jp.kshoji.driver.midi.device.MidiInputDevice,
        cable: Int,
        channel: Int,
        note: Int,
        velocity: Int
    ) {
        if (isBleOutputConnected) midiOutputDevice!!.sendMidiNoteOff(channel, note, velocity)
    }

    override fun onMidiPolyphonicAftertouch(
        sender: jp.kshoji.driver.midi.device.MidiInputDevice,
        cable: Int,
        channel: Int,
        note: Int,
        pressure: Int
    ) {
        if (isBleOutputConnected) midiOutputDevice!!.sendMidiPolyphonicAftertouch(
            channel,
            note,
            pressure
        )
    }

    override fun onMidiControlChange(
        sender: jp.kshoji.driver.midi.device.MidiInputDevice,
        cable: Int,
        channel: Int,
        function: Int,
        value: Int
    ) {
        if (isBleOutputConnected) midiOutputDevice!!.sendMidiControlChange(channel, function, value)
    }

    override fun onMidiProgramChange(
        sender: jp.kshoji.driver.midi.device.MidiInputDevice,
        cable: Int,
        channel: Int,
        program: Int
    ) {
        if (isBleOutputConnected) midiOutputDevice!!.sendMidiProgramChange(channel, program)
    }

    override fun onMidiChannelAftertouch(
        sender: jp.kshoji.driver.midi.device.MidiInputDevice,
        cable: Int,
        channel: Int,
        pressure: Int
    ) {
        if (isBleOutputConnected) midiOutputDevice!!.sendMidiChannelAftertouch(channel, pressure)
    }

    override fun onMidiPitchWheel(
        sender: jp.kshoji.driver.midi.device.MidiInputDevice,
        cable: Int,
        channel: Int,
        amount: Int
    ) {
        if (isBleOutputConnected) midiOutputDevice!!.sendMidiPitchWheel(channel, amount)
    }

    override fun onMidiTimeCodeQuarterFrame(
        sender: jp.kshoji.driver.midi.device.MidiInputDevice,
        cable: Int,
        timing: Int
    ) {
        if (isBleOutputConnected) midiOutputDevice!!.sendMidiTimeCodeQuarterFrame(timing)
    }

    override fun onMidiSongSelect(
        sender: jp.kshoji.driver.midi.device.MidiInputDevice,
        cable: Int,
        song: Int
    ) {
        if (isBleOutputConnected) midiOutputDevice!!.sendMidiSongSelect(song)
    }

    override fun onMidiSongPositionPointer(
        sender: jp.kshoji.driver.midi.device.MidiInputDevice,
        cable: Int,
        position: Int
    ) {
        if (isBleOutputConnected) midiOutputDevice!!.sendMidiSongPositionPointer(position)
    }

    override fun onMidiTuneRequest(
        sender: jp.kshoji.driver.midi.device.MidiInputDevice,
        cable: Int
    ) {
        if (isBleOutputConnected) midiOutputDevice!!.sendMidiTuneRequest()
    }

    override fun onMidiTimingClock(
        sender: jp.kshoji.driver.midi.device.MidiInputDevice,
        cable: Int
    ) {
        if (isBleOutputConnected) midiOutputDevice!!.sendMidiTimingClock()
    }

    override fun onMidiStart(sender: jp.kshoji.driver.midi.device.MidiInputDevice, cable: Int) {
        if (isBleOutputConnected) midiOutputDevice!!.sendMidiStart()
    }

    override fun onMidiContinue(sender: jp.kshoji.driver.midi.device.MidiInputDevice, cable: Int) {
        if (isBleOutputConnected) midiOutputDevice!!.sendMidiContinue()
    }

    override fun onMidiStop(sender: jp.kshoji.driver.midi.device.MidiInputDevice, cable: Int) {
        if (isBleOutputConnected) midiOutputDevice!!.sendMidiStop()
    }

    override fun onMidiActiveSensing(
        sender: jp.kshoji.driver.midi.device.MidiInputDevice,
        cable: Int
    ) {
        if (isBleOutputConnected) midiOutputDevice!!.sendMidiActiveSensing()
    }

    override fun onMidiReset(sender: jp.kshoji.driver.midi.device.MidiInputDevice, cable: Int) {
        if (isBleOutputConnected) midiOutputDevice!!.sendMidiReset()
    }
}