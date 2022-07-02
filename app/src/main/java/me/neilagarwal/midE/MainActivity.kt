package me.neilagarwal.midE

import android.Manifest
import jp.kshoji.driver.midi.activity.AbstractSingleMidiActivity
import jp.kshoji.blemidi.peripheral.BleMidiPeripheralProvider
import jp.kshoji.blemidi.central.BleMidiCentralProvider
import android.os.Bundle
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import android.hardware.usb.UsbDevice
import android.os.Handler
import android.os.Message
import android.view.View
import jp.kshoji.blemidi.device.MidiInputDevice
import jp.kshoji.blemidi.device.MidiOutputDevice
import jp.kshoji.blemidi.listener.OnMidiDeviceAttachedListener
import jp.kshoji.blemidi.listener.OnMidiDeviceDetachedListener
import me.neilagarwal.midE.databinding.ActivityMainBinding

class MainActivity : AbstractSingleMidiActivity() {
    private var bleMidiPeripheralProvider: BleMidiPeripheralProvider? = null
    private var bleMidiCentralProvider: BleMidiCentralProvider? = null
    var midiOutputDevice: MidiOutputDevice? = null

    private var binding: ActivityMainBinding? = null

    var advertising = false
    var isBleOutputConnected = false

    var usbMidiDeviceName = "[none]"
    var bleMidiDeviceName = "[none]"

    // this field belongs to the UI thread
    val uiThreadEventHandler = Handler { msg -> // USB Device Name
        if (msg.what == 0) {
            binding!!.usbMidiDeviceName.text = "USB In/Out: $usbMidiDeviceName"
        }

        // BLE Device Name
        if (msg.what == 1) {
            binding!!.bleMidiDeviceName.text = "BLE Out: " + midiOutputDevice!!.deviceName
        }

        // message handled successfully
        true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view: View = binding!!.root
        setContentView(view)
        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.BLUETOOTH_ADVERTISE), 2)
                return
            }
        }
        bleMidiPeripheralProvider = BleMidiPeripheralProvider(this)
        bleMidiPeripheralProvider!!.setDeviceName(usbMidiDeviceName!!)
        bleMidiPeripheralProvider!!.setManufacturer("midE")
        bleMidiCentralProvider = BleMidiCentralProvider(this)
        binding!!.button.setOnClickListener {
            if (!advertising) {
                bleMidiPeripheralProvider!!.startAdvertising()
                advertising = true
                binding!!.button.text = "Stop Advertising"
            } else {
                bleMidiPeripheralProvider!!.stopAdvertising()
                advertising = false
                binding!!.button.text = "Start Advertising"
            }
        }

        // Listener for Device disconnection
        bleMidiPeripheralProvider!!.setOnMidiDeviceAttachedListener(object : OnMidiDeviceAttachedListener {
            override fun onMidiInputDeviceAttached(midiInputDevice: MidiInputDevice) {}
            override fun onMidiOutputDeviceAttached(localMidiOutputDevice: MidiOutputDevice) {
                midiOutputDevice = localMidiOutputDevice
                isBleOutputConnected = true
                bleMidiDeviceName = midiOutputDevice!!.deviceName
                uiThreadEventHandler.sendMessage(Message.obtain(uiThreadEventHandler, 1))
            }
        })
        bleMidiPeripheralProvider!!.setOnMidiDeviceDetachedListener(object : OnMidiDeviceDetachedListener {
            override fun onMidiInputDeviceDetached(midiInputDevice: MidiInputDevice) {}
            override fun onMidiOutputDeviceDetached(localMidiOutputDevice: MidiOutputDevice) {
                isBleOutputConnected = false
            }
        })
    }

    override fun onDeviceAttached(usbDevice: UsbDevice) {}
    override fun onMidiInputDeviceAttached(midiInputDevice: jp.kshoji.driver.midi.device.MidiInputDevice) {}
    override fun onMidiOutputDeviceAttached(midiOutputDevice: jp.kshoji.driver.midi.device.MidiOutputDevice) {
        usbMidiDeviceName = midiOutputDevice.productName!!
        uiThreadEventHandler.sendMessage(Message.obtain(uiThreadEventHandler, 0))
    }

    override fun onDeviceDetached(usbDevice: UsbDevice) {}
    override fun onMidiInputDeviceDetached(midiInputDevice: jp.kshoji.driver.midi.device.MidiInputDevice) {}
    override fun onMidiOutputDeviceDetached(midiOutputDevice: jp.kshoji.driver.midi.device.MidiOutputDevice) {
        usbMidiDeviceName = "[none]"
        uiThreadEventHandler.sendMessage(Message.obtain(uiThreadEventHandler, 0))
    }

    override fun onMidiMiscellaneousFunctionCodes(sender: jp.kshoji.driver.midi.device.MidiInputDevice, cable: Int, byte1: Int, byte2: Int, byte3: Int) {}
    override fun onMidiCableEvents(sender: jp.kshoji.driver.midi.device.MidiInputDevice, cable: Int, byte1: Int, byte2: Int, byte3: Int) {}
    override fun onMidiSystemCommonMessage(sender: jp.kshoji.driver.midi.device.MidiInputDevice, cable: Int, bytes: ByteArray) {}
    override fun onMidiSingleByte(sender: jp.kshoji.driver.midi.device.MidiInputDevice, cable: Int, byte1: Int) {}

    override fun onMidiSystemExclusive(sender: jp.kshoji.driver.midi.device.MidiInputDevice, cable: Int, systemExclusive: ByteArray) {
        if (isBleOutputConnected) midiOutputDevice!!.sendMidiSystemExclusive(systemExclusive)
    }

    override fun onMidiNoteOn(sender: jp.kshoji.driver.midi.device.MidiInputDevice, cable: Int, channel: Int, note: Int, velocity: Int) {
        if (isBleOutputConnected) midiOutputDevice!!.sendMidiNoteOn(channel, note, velocity)
    }

    override fun onMidiNoteOff(sender: jp.kshoji.driver.midi.device.MidiInputDevice, cable: Int, channel: Int, note: Int, velocity: Int) {
        if (isBleOutputConnected) midiOutputDevice!!.sendMidiNoteOff(channel, note, velocity)
    }

    override fun onMidiPolyphonicAftertouch(sender: jp.kshoji.driver.midi.device.MidiInputDevice, cable: Int, channel: Int, note: Int, pressure: Int) {
        if (isBleOutputConnected) midiOutputDevice!!.sendMidiPolyphonicAftertouch(channel, note, pressure)
    }

    override fun onMidiControlChange(sender: jp.kshoji.driver.midi.device.MidiInputDevice, cable: Int, channel: Int, function: Int, value: Int) {
        if (isBleOutputConnected) midiOutputDevice!!.sendMidiControlChange(channel, function, value)
    }

    override fun onMidiProgramChange(sender: jp.kshoji.driver.midi.device.MidiInputDevice, cable: Int, channel: Int, program: Int) {
        if (isBleOutputConnected) midiOutputDevice!!.sendMidiProgramChange(channel, program)
    }

    override fun onMidiChannelAftertouch(sender: jp.kshoji.driver.midi.device.MidiInputDevice, cable: Int, channel: Int, pressure: Int) {
        if (isBleOutputConnected) midiOutputDevice!!.sendMidiChannelAftertouch(channel, pressure)
    }

    override fun onMidiPitchWheel(sender: jp.kshoji.driver.midi.device.MidiInputDevice, cable: Int, channel: Int, amount: Int) {
        if (isBleOutputConnected) midiOutputDevice!!.sendMidiPitchWheel(channel, amount)
    }

    override fun onMidiTimeCodeQuarterFrame(sender: jp.kshoji.driver.midi.device.MidiInputDevice, cable: Int, timing: Int) {
        if (isBleOutputConnected) midiOutputDevice!!.sendMidiTimeCodeQuarterFrame(timing)
    }

    override fun onMidiSongSelect(sender: jp.kshoji.driver.midi.device.MidiInputDevice, cable: Int, song: Int) {
        if (isBleOutputConnected) midiOutputDevice!!.sendMidiSongSelect(song)
    }

    override fun onMidiSongPositionPointer(sender: jp.kshoji.driver.midi.device.MidiInputDevice, cable: Int, position: Int) {
        if (isBleOutputConnected) midiOutputDevice!!.sendMidiSongPositionPointer(position)
    }

    override fun onMidiTuneRequest(sender: jp.kshoji.driver.midi.device.MidiInputDevice, cable: Int) {
        if (isBleOutputConnected) midiOutputDevice!!.sendMidiTuneRequest()
    }

    override fun onMidiTimingClock(sender: jp.kshoji.driver.midi.device.MidiInputDevice, cable: Int) {
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

    override fun onMidiActiveSensing(sender: jp.kshoji.driver.midi.device.MidiInputDevice, cable: Int) {
        if (isBleOutputConnected) midiOutputDevice!!.sendMidiActiveSensing()
    }

    override fun onMidiReset(sender: jp.kshoji.driver.midi.device.MidiInputDevice, cable: Int) {
        if (isBleOutputConnected) midiOutputDevice!!.sendMidiReset()
    }
}