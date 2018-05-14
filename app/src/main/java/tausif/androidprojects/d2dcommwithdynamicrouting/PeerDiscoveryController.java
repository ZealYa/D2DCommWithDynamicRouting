package tausif.androidprojects.d2dcommwithdynamicrouting;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import android.net.wifi.p2p.WifiP2pDevice;

public class PeerDiscoveryController {
    private Context context;
    private HomeActivity homeActivity;
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel channel;
    private BluetoothAdapter bluetoothAdapter;
    private PeerDiscoveryBroadcastReceiver peerDiscoveryBroadcastReceiver;
    private IntentFilter intentFilter;
    private ArrayList<Device> wifiDevices;
    private ArrayList<Device> bluetoothDevices;

    public PeerDiscoveryController(Context context, HomeActivity homeActivity) {
        this.context = context;
        this.homeActivity = homeActivity;
        peerDiscoveryBroadcastReceiver = new PeerDiscoveryBroadcastReceiver();
        peerDiscoveryBroadcastReceiver.setPeerDiscoveryController(this);
        intentFilter = new IntentFilter();
        configureWiFiDiscovery();
        configureBluetoothDiscovery();
        context.registerReceiver(peerDiscoveryBroadcastReceiver, intentFilter);
        wifiDevices = new ArrayList<>();
        wifiP2pManager.discoverPeers(channel, null);
        Constants.timeSlotCount = 0;
        Timer timer = new Timer();
        int timeInterval = 30;
        timer.scheduleAtFixedRate(new controlPeerDiscovery(), 0, timeInterval*1000);
    }

    private void configureWiFiDiscovery() {
        wifiP2pManager = (WifiP2pManager)context.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(context, context.getMainLooper(), null);
        peerDiscoveryBroadcastReceiver.setWifiP2pManager(wifiP2pManager);
        peerDiscoveryBroadcastReceiver.setChannel(channel);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    private void configureBluetoothDiscovery() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Constants.hostBluetoothAddress = bluetoothAdapter.getAddress();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
    }

    public void wifiDeviceDiscovered(WifiP2pDeviceList deviceList) {
        if (wifiDevices.size()>0)
            wifiDevices.clear();
        for (WifiP2pDevice device: deviceList.getDeviceList()
             ) {
            Device newDevice = new Device(Constants.WIFI_DEVICE, device, null, 0);
            wifiDevices.add(newDevice);
        }
    }

    public void bluetoothDeviceDiscovered(BluetoothDevice device, int rssi) {
        int flag = 0;
        Device newDevice = new Device(Constants.BLUETOOTH_DEVICE, null, device, rssi);
        for (Device oldDevice: bluetoothDevices
             ) {
            if (newDevice.bluetoothDevice.getAddress().equalsIgnoreCase(oldDevice.bluetoothDevice.getAddress())) {
                flag = 1;
                break;
            }
        }
        if (flag == 0)
            bluetoothDevices.add(newDevice);
    }

    private class controlPeerDiscovery extends TimerTask {
        @Override
        public void run() {
            if (Constants.timeSlotCount%2==0){
                bluetoothDevices = new ArrayList<>();
                bluetoothAdapter.startDiscovery();
            } else {
                bluetoothAdapter.cancelDiscovery();
                homeActivity.discoveryFinished(wifiDevices, bluetoothDevices);
            }
            Constants.timeSlotCount++;
        }
    }
}
