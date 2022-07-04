package me.neilagarwal.midE

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class MainViewModel: ViewModel() {
    val noDeviceDeviceName: String = "[none]"

    var usbMidiDeviceName by mutableStateOf(noDeviceDeviceName)
    var bleMidiDevicename by mutableStateOf(noDeviceDeviceName)

    var testing by mutableStateOf("testing")
}