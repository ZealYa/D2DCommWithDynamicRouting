package tausif.androidprojects.d2dcommwithdynamicrouting;

import android.bluetooth.BluetoothDevice;
import android.net.wifi.p2p.WifiP2pDevice;

import java.net.InetAddress;
import java.util.Date;


public class Device {
    public int deviceType;
    public WifiP2pDevice wifiDevice;
    public BluetoothDevice bluetoothDevice;
    private int rssi;
    public long roundTripTime;
    public long rttStartTime;
    public String rttPkt;
    public double packetLossRatio;
    public boolean connected;
    public InetAddress IPAddress;


    public Device(int deviceType, WifiP2pDevice wifiDevice, BluetoothDevice bluetoothDevice, int rssi, boolean connected) {
        this.deviceType = deviceType;
        this.wifiDevice = wifiDevice;
        this.bluetoothDevice = bluetoothDevice;
        this.rssi = rssi;
        this.roundTripTime = 0;
        this.rttStartTime = 0;
        this.rttPkt = "";
        this.packetLossRatio = 0;
        this.connected = connected;
        this.IPAddress = null;
    }
}
