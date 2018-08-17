package tausif.androidprojects.d2dcommwithdynamicrouting;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Calendar;

public class HomeActivity extends AppCompatActivity {

    ArrayList<Device> combinedDeviceList;
    ArrayList<Device> wifiDevices;
    ArrayList<Device> bluetoothDevices;
    ListView deviceListView;
    DeviceListAdapter deviceListAdapter;
    PeerDiscoveryController peerDiscoveryController;
    WDUDPSender udpSender;
    boolean willUpdateDeviceList;
    int experimentNo;
    long RTTs[];
    long udpThroughputRTTs[];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        checkStorageWritePermission();
        willUpdateDeviceList = true;
        startDiscovery();
    }

    public void checkStorageWritePermission() {
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constants.REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION);
        }
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
        EditText distanceText = findViewById(R.id.distance_editText);
        if (textboxIsEmpty(distanceText)) {
            distanceText.setError("enter distance");
            return;
        }
        EditText pktSizeText = findViewById(R.id.pkt_size_editText);
        if (textboxIsEmpty(pktSizeText)) {
            pktSizeText.setError("enter packet size");
            return;
        }
        if (currentDevice.IPAddress == null) {
            Toast.makeText(this, "ip mac not synced", Toast.LENGTH_LONG).show();
            return;
        }
        experimentNo = 0;
        RTTs = new long[Constants.highestNoOfRuns];
        String pktSizeStr = pktSizeText.getText().toString().trim();
        int pktSize = Integer.parseInt(pktSizeStr);
        currentDevice.rttPkt = PacketManager.createRTTPacket(Constants.RTT, Constants.hostWifiAddress, currentDevice.wifiDevice.deviceAddress, pktSize);
        udpSender = null;
        udpSender = new WDUDPSender();
        udpSender.createPkt(currentDevice.rttPkt, currentDevice.IPAddress);
        udpSender.setRunLoop(false);
        currentDevice.rttStartTime = Calendar.getInstance().getTimeInMillis();
        udpSender.start();
    }

    public void pktLossButton(View view) {
        int tag = (int)view.getTag();
        Device currentDevice = combinedDeviceList.get(tag);
        EditText distanceText = findViewById(R.id.distance_editText);
        if (textboxIsEmpty(distanceText)) {
            distanceText.setError("enter distance");
            return;
        }
        if (currentDevice.IPAddress == null) {
            Toast.makeText(this, "ip mac not synced", Toast.LENGTH_LONG).show();
            return;
        }
        currentDevice.setLossRatioPktsReceived(0);
        udpSender = null;
        udpSender = new WDUDPSender();
        String lossRatioPkt = PacketManager.createLossRatioPacket(Constants.PKT_LOSS, Constants.hostWifiAddress, currentDevice.wifiDevice.deviceAddress);
        udpSender.createPkt(lossRatioPkt, currentDevice.IPAddress);
        udpSender.setRunLoop(true);
        udpSender.setNoOfPktsToSend(Constants.MAX_LOSS_RATIO_PKTS);
        udpSender.start();
    }

    public void UDPThroughputButton(View view) {
        int tag = (int)view.getTag();
        Device currentDevice = combinedDeviceList.get(tag);
        EditText distanceText = findViewById(R.id.distance_editText);
        if (textboxIsEmpty(distanceText)) {
            distanceText.setError("enter distance");
            return;
        }
        if (currentDevice.IPAddress == null) {
            Toast.makeText(this, "ip mac not synced", Toast.LENGTH_LONG).show();
            return;
        }
        experimentNo = 0;
        udpThroughputRTTs = new long[Constants.highestNoOfRuns +1];
    }

    public void TCPThroughputButton(View view) {
        int tag = (int)view.getTag();
    }

    //callback method from peer discovery controller after finishing a cycle of wifi and bluetooth discovery
    public void discoveryFinished(ArrayList<Device> wifiDevices, ArrayList<Device> bluetoothDevices) {
        if (willUpdateDeviceList) {
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
    }

    //shows the wifi p2p state
    public void wifiP2PState(int state) {
        if (state == 0)
            showAlert("WiFi direct disabled");
    }

    public void connectionEstablished(int connectionType) {
        if (connectionType == Constants.WIFI_DIRECT_CONNECTION) {
            Toast.makeText(this, "connection established", Toast.LENGTH_LONG).show();
            WDUDPListener udpListener = new WDUDPListener(this);
            udpListener.start();
            if (!Constants.isGroupOwner)
                ipMacSync();
        }
    }

    public void ipMacSync() {
        String pkt = PacketManager.createIpMacSyncPkt(Constants.IP_MAC_SYNC_REC, Constants.hostWifiAddress);
        udpSender = null;
        udpSender = new WDUDPSender();
        udpSender.createPkt(pkt, Constants.groupOwnerAddress);
        udpSender.setRunLoop(false);
        udpSender.start();
    }

    public void matchIPToMac(InetAddress ipAddr, String macAddr) {
        for (Device device:combinedDeviceList
                ) {
            if (device.deviceType == Constants.WIFI_DEVICE) {
                if (device.wifiDevice.deviceAddress.equals(macAddr)){
                    device.IPAddress = ipAddr;
                    willUpdateDeviceList = false;
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

    public void processReceivedWiFiPkt(InetAddress srcAddr, long receivingTime, final String receivedPkt) {
        String splited[] = receivedPkt.split("#");
        int pktType = Integer.parseInt(splited[0]);
        if (pktType == Constants.IP_MAC_SYNC_REC) {
            String pkt = PacketManager.createIpMacSyncPkt(Constants.IP_MAC_SYNC_RET, Constants.hostWifiAddress);
            udpSender = null;
            udpSender = new WDUDPSender();
            udpSender.createPkt(pkt, srcAddr);
            udpSender.setRunLoop(false);
            udpSender.start();
            matchIPToMac(srcAddr, splited[1]);
        }
        else if (pktType == Constants.IP_MAC_SYNC_RET)
            matchIPToMac(srcAddr, splited[1]);
        else if (pktType == Constants.RTT) {
            int pktSize = Integer.parseInt(splited[3]);
            String pkt = PacketManager.createRTTPacket(Constants.RTT_RET, Constants.hostWifiAddress, splited[1], pktSize);
            udpSender = null;
            udpSender = new WDUDPSender();
            udpSender.createPkt(pkt, srcAddr);
            udpSender.setRunLoop(false);
            udpSender.start();
        }
        else if (pktType == Constants.RTT_RET) {
            for (Device device:combinedDeviceList
                 ) {
                if (device.deviceType == Constants.WIFI_DEVICE) {
                    if (device.wifiDevice.deviceAddress.equals(splited[1])) {
                        device.roundTripTime = receivingTime - device.rttStartTime;
                        RTTs[experimentNo] = device.roundTripTime;
                        experimentNo++;
                        if (experimentNo < Constants.highestNoOfRuns) {
                            udpSender = null;
                            udpSender = new WDUDPSender();
                            udpSender.createPkt(device.rttPkt, srcAddr);
                            udpSender.setRunLoop(false);
                            device.rttStartTime = Calendar.getInstance().getTimeInMillis();
                            udpSender.start();
                        }
                        else {
                            writeResult(device.wifiDevice.deviceName, Constants.RTT);
                        }
                        break;
                    }
                }
            }
        }
    }

    public void writeResult(String deviceName, int measurementType) {
        EditText distanceText = findViewById(R.id.distance_editText);
        String distance = distanceText.getText().toString().trim();

        EditText pktSizeText = findViewById(R.id.pkt_size_editText);
        String pktSize = pktSizeText.getText().toString().trim();

        if (measurementType == Constants.RTT) {
            boolean retVal = FileWriter.writeRTTResult(deviceName, pktSize, distance, RTTs);
            if (retVal)
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), Constants.writeSuccess, Toast.LENGTH_LONG).show();
                    }
                });
            else
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), Constants.writeFail, Toast.LENGTH_LONG).show();
                    }
                });
        }
    }

    //function to show an alert message
    public void showAlert(String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    //function to check empty text field
    public boolean textboxIsEmpty(EditText editText) {
        return editText.getText().toString().trim().length() == 0;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}