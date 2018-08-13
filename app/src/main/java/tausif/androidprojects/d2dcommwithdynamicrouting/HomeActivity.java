package tausif.androidprojects.d2dcommwithdynamicrouting;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;

import tausif.androidprojects.files.TransferService;

public class HomeActivity extends AppCompatActivity {

    ArrayList<Device> combinedDeviceList;
    ArrayList<Device> wifiDevices;
    ArrayList<Device> bluetoothDevices;
    ListView deviceListView;
    DeviceListAdapter deviceListAdapter;
    PeerDiscoveryController peerDiscoveryController;
    WiFiDirectUDPSender udpSender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        startDiscovery();
    }

    //configures the bluetooth and wifi discovery options and starts the background process for discovery
    public void startDiscovery(){
        configureDeviceListView();
        peerDiscoveryController = new PeerDiscoveryController(this, this);
    }

    //setting up the device list view adapter and item click events
    public void configureDeviceListView(){
        deviceListView = findViewById(R.id.device_listView);
        combinedDeviceList = new ArrayList<>();
        deviceListAdapter = new DeviceListAdapter(this, combinedDeviceList);
        deviceListView.setAdapter(deviceListAdapter);
    }

    public void connectButton(View view) {
        int tag = (int)view.getTag();
        Device currentDevice = combinedDeviceList.get(tag);
        if (currentDevice.deviceType == Constants.WIFI_DEVICE)
            peerDiscoveryController.connectWiFiDirectDevice(combinedDeviceList.get(tag));
    }

    public void rttButton(View view) {
        int tag = (int)view.getTag();
        Device currentDevice = combinedDeviceList.get(tag);
        if (currentDevice.deviceType == Constants.WIFI_DEVICE){
            if (!Constants.isGroupOwner) {
                ipMacSync();
            }
        }
    }

    public void pktLossButton(View view) {
        int tag = (int)view.getTag();
    }

    public void throughputButton(View view) {
        int tag = (int)view.getTag();
    }

    //callback method from peer discovery controller after finishing a cycle of wifi and bluetooth discovery
    public void discoveryFinished(ArrayList<Device> wifiDevices, ArrayList<Device> bluetoothDevices) {
        this.wifiDevices = wifiDevices;
        this.bluetoothDevices = bluetoothDevices;
        if (combinedDeviceList.size() > 0)
            combinedDeviceList.clear();
        combinedDeviceList.addAll(this.wifiDevices);
        combinedDeviceList.addAll(this.bluetoothDevices);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                deviceListAdapter.notifyDataSetChanged();
            }
        });
    }

    //shows the wifi p2p state
    public void wifiP2PState(int state) {
        if (state == 0)
            showAlert("WiFi direct disabled");
    }

    public void connectionEstablished(int connectionType) {
        if (connectionType == Constants.WIFI_DIRECT_CONNECTION) {
            Toast.makeText(this, "connection established", Toast.LENGTH_LONG).show();
            udpSender = new WiFiDirectUDPSender();
            WiFiDirectUDPListener udpListener = new WiFiDirectUDPListener(this);
            udpListener.start();
            if (!Constants.isGroupOwner)
                ipMacSync();
        }
    }

    public void ipMacSync() {
        String pkt = PacketManager.createIpMacSyncPkt(Constants.IP_MAC_SYNC_REC, Constants.hostWifiAddress);
        udpSender = new WiFiDirectUDPSender();
        udpSender.createPkt(pkt, Constants.groupOwnerAddress);
        udpSender.start();
    }

    public void matchIPToMac(InetAddress ipAddr, String macAddr) {
        for (Device device:combinedDeviceList
                ) {
            if (device.deviceType == Constants.WIFI_DEVICE) {
                if (device.wifiDevice.deviceAddress.equals(macAddr)){
                    device.IPAddress = ipAddr;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "ip mac synced", Toast.LENGTH_LONG).show();
                        }
                    });
                    break;
                }
            }
        }
    }

    public void processReceivedWiFiPkt(InetAddress srcAddr, long receivingTime, String receivedPkt) {
        String splited[] = receivedPkt.split("#");
        int pktType = Integer.parseInt(splited[0]);
        if (pktType == Constants.IP_MAC_SYNC_REC) {
            String pkt = PacketManager.createIpMacSyncPkt(Constants.IP_MAC_SYNC_RET, Constants.hostWifiAddress);
            udpSender.createPkt(pkt, srcAddr);
            udpSender.start();
            matchIPToMac(srcAddr, splited[1]);
        }
        else if (pktType == Constants.IP_MAC_SYNC_RET)
            matchIPToMac(srcAddr, splited[1]);
    }

    //function to show an alert message
    public void showAlert(String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}