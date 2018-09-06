package tausif.androidprojects.d2dcommwithdynamicrouting;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;

import java.util.ArrayList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import android.net.wifi.p2p.WifiP2pDevice;

public class PeerDiscoveryController implements WifiP2pManager.ConnectionInfoListener {
    private Context context;
    private HomeActivity homeActivity;
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel channel;
    private BluetoothAdapter bluetoothAdapter;
    private PeerDiscoveryBroadcastReceiver peerDiscoveryBroadcastReceiver;
    private IntentFilter intentFilter;
    private ArrayList<Device> wifiDevices;
    private ArrayList<Device> bluetoothDevices;
    private int timeSlotNo;

    PeerDiscoveryController(Context context, HomeActivity homeActivity) {
        this.context = context;
        this.homeActivity = homeActivity;
        peerDiscoveryBroadcastReceiver = new PeerDiscoveryBroadcastReceiver();
        peerDiscoveryBroadcastReceiver.setPeerDiscoveryController(this);
        peerDiscoveryBroadcastReceiver.setSourceActivity(this.homeActivity);
        intentFilter = new IntentFilter();
        configureWiFiDiscovery();
        configureBluetoothDiscovery();
        context.registerReceiver(peerDiscoveryBroadcastReceiver, intentFilter);
        wifiDevices = new ArrayList<>();
        wifiP2pManager.discoverPeers(channel, null);
        timeSlotNo = 0;
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new controlPeerDiscovery(), 0, Constants.TIME_SLOT_LENGTH *1000);
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
            Device newDevice = new Device(Constants.WIFI_DEVICE, device, null, 0, false);
            wifiDevices.add(newDevice);
        }
    }

    public void bluetoothDeviceDiscovered(BluetoothDevice device, int rssi) {
        Device newDevice = new Device(Constants.BLUETOOTH_DEVICE, null, device, rssi, false);
        bluetoothDevices.add(newDevice);
    }

    private class controlPeerDiscovery extends TimerTask {
        @Override
        public void run() {
            if (timeSlotNo %2==0){
                bluetoothDevices = new ArrayList<>();
                // adding up already paired devices
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                if (pairedDevices.size() > 0) {
                    for (BluetoothDevice pairedDevice: pairedDevices
                         ) {
                        Device device = new Device(Constants.BLUETOOTH_DEVICE, null, pairedDevice, 0, true);
                        bluetoothDevices.add(device);
                    }
                }
                bluetoothAdapter.startDiscovery();
                if (Constants.willRecordRSSI)
                    Constants.NO_OF_EXPS--;
            } else {
                bluetoothAdapter.cancelDiscovery();
                homeActivity.discoveryFinished(wifiDevices, bluetoothDevices);
            }
            timeSlotNo++;
        }
    }

    public void connectWiFiDirectDevice(Device device) {
        if (device.deviceType == Constants.WIFI_DEVICE) {
            WifiP2pConfig newConfig = new WifiP2pConfig();
            newConfig.deviceAddress = device.wifiDevice.deviceAddress;
            newConfig.wps.setup = WpsInfo.PBC;
            wifiP2pManager.connect(channel, newConfig, null);
        }
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        Constants.groupOwnerAddress = wifiP2pInfo.groupOwnerAddress;
        Constants.isGroupOwner = wifiP2pInfo.isGroupOwner;
        homeActivity.connectionEstablished(Constants.WIFI_DIRECT_CONNECTION);
    }
}
