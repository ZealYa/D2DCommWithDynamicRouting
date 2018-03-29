package tausif.androidprojects.d2dcommwithdynamicrouting;

import android.bluetooth.BluetoothDevice;



public class Device {
    public String deviceName;
    public String deviceAddress;
    public int selected;
    public int deviceType;
    public BluetoothDevice bluetoothDevice;

    public Device(String deviceName, String deviceAddress, int selected, int deviceType, BluetoothDevice bluetoothDevice) {
        this.deviceName = deviceName;
        this.deviceAddress = deviceAddress;
        this.selected = selected;
        this.deviceType = deviceType;
        this.bluetoothDevice = bluetoothDevice;
    }
}
