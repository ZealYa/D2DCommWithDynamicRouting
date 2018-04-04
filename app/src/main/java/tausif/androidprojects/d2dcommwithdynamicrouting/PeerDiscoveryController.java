package tausif.androidprojects.d2dcommwithdynamicrouting;

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
    private int timeSlotCount;
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel channel;
    private PeerDiscoveryBroadcastReceiver broadcastReceiver;
    private IntentFilter intentFilter;
    private ArrayList<Device> wifiDevices;

    public PeerDiscoveryController(Context context, HomeActivity homeActivity) {
        this.context = context;
        this.homeActivity = homeActivity;
        broadcastReceiver = new PeerDiscoveryBroadcastReceiver();
        intentFilter = new IntentFilter();
        Timer timer = new Timer();
        int timeInterval = 10;
        timer.scheduleAtFixedRate(new controlPeerDiscovery(), 0, timeInterval*1000);
    }

    public void configureWiFiDiscovery() {
        wifiP2pManager = (WifiP2pManager)context.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(context, context.getMainLooper(), null);
        broadcastReceiver.setWifiP2pManager(wifiP2pManager);
        broadcastReceiver.setChannel(channel);
        broadcastReceiver.setPeerDiscoveryController(this);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    public void wifiDeviceDiscovered(WifiP2pDeviceList deviceList) {
        if (wifiDevices.size()>0)
            wifiDevices.clear();
        for (WifiP2pDevice device: deviceList.getDeviceList()
             ) {
            Device newDevice = new Device(device.deviceName, device.deviceAddress, 0, 0, null, 0);
            wifiDevices.add(newDevice);
        }
    }

    private class controlPeerDiscovery extends TimerTask {
        @Override
        public void run() {
            if (timeSlotCount%2==0){
                wifiDevices = new ArrayList<>();
                wifiP2pManager.discoverPeers(channel, null);
            } else {
                wifiP2pManager.stopPeerDiscovery(channel, null);
            }
            timeSlotCount++;
        }
    }
}
