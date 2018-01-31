package tausif.androidprojects.d2dcommwithdynamicrouting;

import android.bluetooth.BluetoothDevice;



public class Device {
    public String deviceName;
    public String deviceAddress;
    public int selected;
    public BluetoothDevice bluetoothDevice;

    public Device(String deviceName, String deviceAddress, int selected, BluetoothDevice bluetoothDevice) {
        this.deviceName = deviceName;
        this.deviceAddress = deviceAddress;
        this.selected = selected;
        this.bluetoothDevice = bluetoothDevice;
    }
}
