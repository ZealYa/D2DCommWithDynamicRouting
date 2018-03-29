package tausif.androidprojects.d2dcommwithdynamicrouting;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by ctaus on 3/21/2018.
 * this class contains the methods to control the peer discovery process for bluetooth and wifi
 */

public class PeerDiscoveryController {

    private HomeActivity homeActivity;
    private int timeSlotCount;
    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<Device> bluetoothDevices;
    private ArrayList<Device> wifiDevices;
    private ArrayList<Device> combinedDeviceList;
    private IntentFilter intentFilter;

    public PeerDiscoveryController(HomeActivity homeActivity) {
        this.homeActivity = homeActivity;
        configureBluetoothDiscovery();
        configureWiFiDiscovery();
        timeSlotCount = 0;
        int timeInterval = 15;
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new StartStopDiscovery(), 0, timeInterval*1000);
    }

    //configure bluetooth device discovery options
    private void configureBluetoothDiscovery() {
        intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    //configure wifi device discovery options
    private void configureWiFiDiscovery() {
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
    }

    class StartStopDiscovery extends TimerTask {
        @Override
        public void run() {
            Log.d("time slot ",String.valueOf(timeSlotCount));
            if (timeSlotCount%2==0) {
                homeActivity.registerReceiver(broadcastReceiver, intentFilter);
                bluetoothDevices = new ArrayList<>();
                //adding up already paired devices`
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                if (pairedDevices.size() > 0) {
                    // There are paired devices. Get the name and address of each paired device.
                    for (BluetoothDevice device : pairedDevices) {
                        Device bluetoothDevice = new Device(device.getName(), device.getAddress(), 0, 1, device);
                        bluetoothDevices.add(bluetoothDevice);
                    }
                }
                bluetoothAdapter.startDiscovery();

                wifiDevices = new ArrayList<>();
            }
            else {
                bluetoothAdapter.cancelDiscovery();
                homeActivity.unregisterReceiver(broadcastReceiver);
                mergeDeviceLists();
                homeActivity.peerDiscoveryFinished(combinedDeviceList);
            }
            timeSlotCount++;
        }
    }

    private void mergeDeviceLists() {
        combinedDeviceList = new ArrayList<>();
        combinedDeviceList.addAll(bluetoothDevices);
        combinedDeviceList.addAll(wifiDevices);
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Device bluetoothDevice = new Device(device.getName(), device.getAddress(), 0, 1, device);
                int flag = 0;
                for (Device item:bluetoothDevices) {
                    if (item.deviceAddress.equalsIgnoreCase(bluetoothDevice.deviceAddress)){
                        flag = 1;
                        break;
                    }
                }
                if (flag == 0){
                    bluetoothDevices.add(bluetoothDevice);
                }
            }
            else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
                    @Override
                    public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
                        for (WifiP2pDevice item : wifiP2pDeviceList.getDeviceList()
                                ) {
                            int flag = 0;
                            for (Device previousDevice: wifiDevices
                                    ) {
                                if (previousDevice.deviceAddress.equalsIgnoreCase(item.deviceAddress))
                                {
                                    flag = 1;
                                    break;
                                }
                            }
                            if (flag == 0) {
                                Device device = new Device(item.deviceName, item.deviceAddress, 0, 0,null);
                                wifiDevices.add(device);
                            }
                        }
                    }
                };
            }
        }
    };
}
