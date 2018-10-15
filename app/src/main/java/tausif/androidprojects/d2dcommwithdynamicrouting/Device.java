package tausif.androidprojects.d2dcommwithdynamicrouting;

import android.bluetooth.BluetoothDevice;
import android.net.wifi.p2p.WifiP2pDevice;
import java.net.InetAddress;


class Device {
    int deviceType;
    WifiP2pDevice wifiDevice;
    BluetoothDevice bluetoothDevice;
    int rssi;
    int lossRatioPktsReceived;
    boolean connected;
    InetAddress IPAddress;


    Device(int deviceType, WifiP2pDevice wifiDevice, BluetoothDevice bluetoothDevice, int rssi, boolean connected) {
        this.deviceType = deviceType;
        this.wifiDevice = wifiDevice;
        this.bluetoothDevice = bluetoothDevice;
        this.rssi = rssi;
        this.connected = connected;
        this.IPAddress = null;
    }
}
