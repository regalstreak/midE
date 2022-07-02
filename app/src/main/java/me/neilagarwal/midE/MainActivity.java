package me.neilagarwal.midE;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import jp.kshoji.blemidi.central.BleMidiCentralProvider;
import jp.kshoji.blemidi.listener.OnMidiDeviceAttachedListener;
import jp.kshoji.blemidi.listener.OnMidiDeviceDetachedListener;
import jp.kshoji.blemidi.peripheral.BleMidiPeripheralProvider;
import jp.kshoji.driver.midi.activity.AbstractSingleMidiActivity;
import jp.kshoji.driver.midi.device.MidiInputDevice;
import jp.kshoji.driver.midi.device.MidiOutputDevice;
import me.neilagarwal.midE.databinding.ActivityMainBinding;

public class MainActivity extends AbstractSingleMidiActivity {

    BleMidiPeripheralProvider bleMidiPeripheralProvider;
    BleMidiCentralProvider bleMidiCentralProvider;

    jp.kshoji.blemidi.device.MidiOutputDevice midiOutputDevice;


   ActivityMainBinding binding;

    boolean advertising = false;
    boolean isBleOutputConnected = false;

    String usbMidiDeviceName = "[none]";
    String bleMidiDeviceName = "[none]";

    // this field belongs to the UI thread
    final Handler uiThreadEventHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            // USB Device Name
            if (msg.what == 0) {
                binding.usbMidiDeviceName.setText("USB In/Out: " + usbMidiDeviceName);
            }

            // BLE Device Name
            if(msg.what == 1) {
                binding.bleMidiDeviceName.setText("BLE Out: " + midiOutputDevice.getDeviceName());
            }

            // message handled successfully
            return true;
        }
    });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_ADVERTISE}, 2);
                return;
            }
        }


        bleMidiPeripheralProvider = new BleMidiPeripheralProvider(this);
        bleMidiPeripheralProvider.setDeviceName(usbMidiDeviceName);
        bleMidiPeripheralProvider.setManufacturer("midE");

        bleMidiCentralProvider = new BleMidiCentralProvider(this);


        binding.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!advertising) {
                    bleMidiPeripheralProvider.startAdvertising();
                    advertising = true;
                    binding.button.setText("Stop Advertising");
                } else {
                    bleMidiPeripheralProvider.stopAdvertising();
                    advertising = false;
                    binding.button.setText("Start Advertising");
                }
            }
        });

        // Listener for Device disconnection
        bleMidiPeripheralProvider.setOnMidiDeviceAttachedListener(new OnMidiDeviceAttachedListener() {
            @Override
            public void onMidiInputDeviceAttached(@NonNull jp.kshoji.blemidi.device.MidiInputDevice midiInputDevice) {
            }

            @Override
            public void onMidiOutputDeviceAttached(@NonNull jp.kshoji.blemidi.device.MidiOutputDevice localMidiOutputDevice) {
                midiOutputDevice = localMidiOutputDevice;
                isBleOutputConnected = true;
                bleMidiDeviceName = midiOutputDevice.getDeviceName();


                uiThreadEventHandler.sendMessage(Message.obtain(uiThreadEventHandler, 1));
            }
        });

        bleMidiPeripheralProvider.setOnMidiDeviceDetachedListener(new OnMidiDeviceDetachedListener() {
            @Override
            public void onMidiInputDeviceDetached(@NonNull jp.kshoji.blemidi.device.MidiInputDevice midiInputDevice) {

            }

            @Override
            public void onMidiOutputDeviceDetached(@NonNull jp.kshoji.blemidi.device.MidiOutputDevice localMidiOutputDevice) {
                isBleOutputConnected = false;
            }
        });
    }


    @Override
    public void onDeviceAttached(@NonNull UsbDevice usbDevice) {

    }

    @Override
    public void onMidiInputDeviceAttached(@NonNull MidiInputDevice midiInputDevice) {

    }

    @Override
    public void onMidiOutputDeviceAttached(@NonNull MidiOutputDevice midiOutputDevice) {
        usbMidiDeviceName = midiOutputDevice.getProductName();
        uiThreadEventHandler.sendMessage(Message.obtain(uiThreadEventHandler, 0));
    }

    @Override
    public void onDeviceDetached(@NonNull UsbDevice usbDevice) {
    }

    @Override
    public void onMidiInputDeviceDetached(@NonNull MidiInputDevice midiInputDevice) {

    }

    @Override
    public void onMidiOutputDeviceDetached(@NonNull MidiOutputDevice midiOutputDevice) {
        usbMidiDeviceName = "[none]";
        uiThreadEventHandler.sendMessage(Message.obtain(uiThreadEventHandler, 0));
    }

    @Override
    public void onMidiMiscellaneousFunctionCodes(@NonNull MidiInputDevice sender, int cable, int byte1, int byte2, int byte3) {

    }

    @Override
    public void onMidiCableEvents(@NonNull MidiInputDevice sender, int cable, int byte1, int byte2, int byte3) {

    }

    @Override
    public void onMidiSystemCommonMessage(@NonNull MidiInputDevice sender, int cable, byte[] bytes) {

    }

    @Override
    public void onMidiSystemExclusive(@NonNull MidiInputDevice sender, int cable, byte[] systemExclusive) {
        if (isBleOutputConnected)
            midiOutputDevice.sendMidiSystemExclusive(systemExclusive);
    }

    @Override
    public void onMidiNoteOn(final MidiInputDevice sender, int cable, int channel, int note, int velocity) {
//        uiThreadEventHandler.sendMessage(Message.obtain(uiThreadEventHandler, 0, "some message"));
        if (isBleOutputConnected)
            midiOutputDevice.sendMidiNoteOn(channel, note, velocity);
    }

    @Override
    public void onMidiNoteOff(@NonNull MidiInputDevice sender, int cable, int channel, int note, int velocity) {
        if (isBleOutputConnected)
            midiOutputDevice.sendMidiNoteOff(channel, note, velocity);
    }

    @Override
    public void onMidiPolyphonicAftertouch(@NonNull MidiInputDevice sender, int cable, int channel, int note, int pressure) {
        if (isBleOutputConnected)
            midiOutputDevice.sendMidiPolyphonicAftertouch(channel, note, pressure);
    }

    @Override
    public void onMidiControlChange(@NonNull MidiInputDevice sender, int cable, int channel, int function, int value) {
        if (isBleOutputConnected)
            midiOutputDevice.sendMidiControlChange(channel, function, value);
    }

    @Override
    public void onMidiProgramChange(@NonNull MidiInputDevice sender, int cable, int channel, int program) {
        if (isBleOutputConnected)
            midiOutputDevice.sendMidiProgramChange(channel, program);
    }

    @Override
    public void onMidiChannelAftertouch(@NonNull MidiInputDevice sender, int cable, int channel, int pressure) {
        if (isBleOutputConnected)
            midiOutputDevice.sendMidiChannelAftertouch(channel, pressure);
    }

    @Override
    public void onMidiPitchWheel(@NonNull MidiInputDevice sender, int cable, int channel, int amount) {
        if (isBleOutputConnected)
            midiOutputDevice.sendMidiPitchWheel(channel, amount);
    }

    @Override
    public void onMidiSingleByte(@NonNull MidiInputDevice sender, int cable, int byte1) {

    }

    @Override
    public void onMidiTimeCodeQuarterFrame(@NonNull MidiInputDevice sender, int cable, int timing) {
        if (isBleOutputConnected)
            midiOutputDevice.sendMidiTimeCodeQuarterFrame(timing);
    }

    @Override
    public void onMidiSongSelect(@NonNull MidiInputDevice sender, int cable, int song) {
        if (isBleOutputConnected)
            midiOutputDevice.sendMidiSongSelect(song);
    }

    @Override
    public void onMidiSongPositionPointer(@NonNull MidiInputDevice sender, int cable, int position) {
        if (isBleOutputConnected)
            midiOutputDevice.sendMidiSongPositionPointer(position);
    }

    @Override
    public void onMidiTuneRequest(@NonNull MidiInputDevice sender, int cable) {
        if (isBleOutputConnected)
            midiOutputDevice.sendMidiTuneRequest();
    }

    @Override
    public void onMidiTimingClock(@NonNull MidiInputDevice sender, int cable) {
        if (isBleOutputConnected)
            midiOutputDevice.sendMidiTimingClock();
    }

    @Override
    public void onMidiStart(@NonNull MidiInputDevice sender, int cable) {
        if (isBleOutputConnected)
            midiOutputDevice.sendMidiStart();
    }

    @Override
    public void onMidiContinue(@NonNull MidiInputDevice sender, int cable) {
        if (isBleOutputConnected)
            midiOutputDevice.sendMidiContinue();
    }

    @Override
    public void onMidiStop(@NonNull MidiInputDevice sender, int cable) {
        if (isBleOutputConnected)
            midiOutputDevice.sendMidiStop();
    }

    @Override
    public void onMidiActiveSensing(@NonNull MidiInputDevice sender, int cable) {
        if (isBleOutputConnected)
            midiOutputDevice.sendMidiActiveSensing();
    }

    @Override
    public void onMidiReset(@NonNull MidiInputDevice sender, int cable) {
        if (isBleOutputConnected)
            midiOutputDevice.sendMidiReset();
    }
}