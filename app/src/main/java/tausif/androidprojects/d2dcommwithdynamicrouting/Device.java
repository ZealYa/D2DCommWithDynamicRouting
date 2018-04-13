package tausif.androidprojects.d2dcommwithdynamicrouting;

import android.bluetooth.BluetoothDevice;
import android.net.wifi.p2p.WifiP2pDevice;


public class Device {
//    public String deviceName;
//    public String deviceAddress;
//    public int selected;
    public int deviceType;
    public WifiP2pDevice wifiDevice;
    public BluetoothDevice bluetoothDevice;
    public int rssi;

//    public Device(String deviceName, String deviceAddress, int selected, int deviceType, BluetoothDevice bluetoothDevice, int rssi) {
//        this.deviceName = deviceName;
//        this.deviceAddress = deviceAddress;
//        this.selected = selected;
//        this.deviceType = deviceType;
//        this.bluetoothDevice = bluetoothDevice;
//        this.rssi = rssi;
//    }

    public Device(int deviceType, WifiP2pDevice wifiDevice, BluetoothDevice bluetoothDevice, int rssi) {
        this.deviceType = deviceType;
        this.wifiDevice = wifiDevice;
        this.bluetoothDevice = bluetoothDevice;
        this.rssi = rssi;
    }
}
