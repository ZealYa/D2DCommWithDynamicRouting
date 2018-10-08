package tausif.androidprojects.d2dcommwithdynamicrouting;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Set;

public class HomeActivity extends AppCompatActivity {

    ArrayList<Device> combinedDeviceList;
    ArrayList<Device> wifiDevices;
    ArrayList<Device> bluetoothDevices;
    ListView deviceListView;
    DeviceListAdapter deviceListAdapter;
    PeerDiscoveryController peerDiscoveryController;
    WDUDPSender udpSender;
    BTConnectedSocketManager btConnectedSocketManager;
    Handler BTDiscoverableHandler;
    boolean willUpdateDeviceList;
    boolean willRecordRSSI;
    int experimentNo;
    long RTTs[];
    int udpThrpughputPktSizes[];
    long udpThroughputRTTs[];
    String distance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        willUpdateDeviceList = true;
        willRecordRSSI = false;
        udpThrpughputPktSizes = new int[] {450, 500, 550, 600, 650, 700, 750, 800, 850, 900};
        setUpPermissions();
        BTDiscoverableHandler = new Handler();
        BTDiscoverableHandler.post(makeBluetoothDiscoverable);
//        setUpBluetoothDataTransfer();
        startDiscovery();
//        getBTPairedDevices();
    }

    public void setUpPermissions() {
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constants.REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION);
        }
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, Constants.REQUEST_CODE_LOCATION);
    }

    private Runnable makeBluetoothDiscoverable = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, Constants.BT_DISCOVERABLE_LENGTH);
            startActivity(intent);
            BTDiscoverableHandler.postDelayed(this, Constants.BT_DISCOVERABLE_LENGTH*1000);
        }
    };

    private void setUpBluetoothDataTransfer() {
        BTConnectionListener btConnectionListener = new BTConnectionListener(this);
        btConnectionListener.start();
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

    public void getBTPairedDevices() {
        configureDeviceListView();
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Constants.hostBluetoothName = bluetoothAdapter.getName();
        bluetoothDevices = new ArrayList<>();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice pairedDevice: pairedDevices
                    ) {
                Device device = new Device(Constants.BLUETOOTH_DEVICE, null, pairedDevice, 0, true);
                bluetoothDevices.add(device);
            }
        }
        combinedDeviceList.addAll(bluetoothDevices);
        deviceListAdapter.notifyDataSetChanged();
    }

    public void recordRSSI(View view) {
        EditText distanceText = (EditText)findViewById(R.id.distance_editText);
        Button recordRSSI = findViewById(R.id.record_rssi_button);
        if (textboxIsEmpty(distanceText))
            distanceText.setError("");
        else {
            distance = distanceText.getText().toString().trim();
            if (willRecordRSSI) {
                willRecordRSSI = false;
                recordRSSI.setText("record rssi");
            }
            else {
                Constants.EXP_NO = 0;
                willRecordRSSI = true;
                recordRSSI.setText("recording rssi");
            }
        }
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
        experimentNo = 0;
        RTTs = new long[Constants.MAX_NO_OF_EXPS];
        String pktSizeStr = pktSizeText.getText().toString().trim();
        int pktSize = Integer.parseInt(pktSizeStr);
        if (currentDevice.deviceType == Constants.WIFI_DEVICE) {
            if (currentDevice.IPAddress == null) {
                showToast("ip mac not synced");
                return;
            }
            currentDevice.rttPkt = PacketManager.createRTTPacket(Constants.RTT, Constants.hostWifiAddress, currentDevice.wifiDevice.deviceAddress, pktSize);
            udpSender = null;
            udpSender = new WDUDPSender();
            udpSender.createPkt(currentDevice.rttPkt, currentDevice.IPAddress);
            udpSender.setRunLoop(false);
            currentDevice.rttStartTime = Calendar.getInstance().getTimeInMillis();
            udpSender.start();
        }
        else {
            Constants.EXP_NO = 0;
            RTTs = new long[Constants.MAX_NO_OF_EXPS];
            BTSocketConnector socketConnector = new BTSocketConnector();
            socketConnector.setDevice(currentDevice);
            BluetoothSocket connectedSocket = socketConnector.createSocket();
            btConnectedSocketManager = null;
            if (connectedSocket!=null) {
                btConnectedSocketManager = new BTConnectedSocketManager(connectedSocket, this);
                btConnectedSocketManager.start();
            }
            btConnectedSocketManager.setDevice(currentDevice);
            calculateBTRTT(currentDevice, pktSize);
        }
    }

    public void calculateBTRTT(Device currentDevice, int pktSize) {
        String packet = PacketManager.createRTTPacket(Constants.RTT, Constants.hostBluetoothName, currentDevice.bluetoothDevice.getName(), pktSize);
        btConnectedSocketManager.sendPkt(packet, Constants.RTT);
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
            showToast("ip mac not synced");
            return;
        }
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
        experimentNo = 0;
        udpThroughputRTTs = new long[Constants.MAX_NO_OF_EXPS];
        int pktSize = udpThrpughputPktSizes[experimentNo];
        if (currentDevice.IPAddress == null) {
            showToast("ip mac not synced");
            return;
        }
        currentDevice.rttPkt = PacketManager.createRTTPacket(Constants.UDP_THROUGHPUT, Constants.hostWifiAddress, currentDevice.wifiDevice.deviceAddress, pktSize);
        udpSender = null;
        udpSender = new WDUDPSender();
        udpSender.createPkt(currentDevice.rttPkt, currentDevice.IPAddress);
        udpSender.setRunLoop(false);
        currentDevice.rttStartTime = Calendar.getInstance().getTimeInMillis();
        udpSender.start();
    }

    //callback method from peer discovery controller after finishing a cycle of wifi and bluetooth discovery
    public void discoveryFinished(ArrayList<Device> wifiDevices, ArrayList<Device> bluetoothDevices) {
        wifiDevices = cleanUpDeviceList(wifiDevices, Constants.WIFI_DEVICE);
        bluetoothDevices = cleanUpDeviceList(bluetoothDevices, Constants.BLUETOOTH_DEVICE);
        if (willUpdateDeviceList) {
            this.wifiDevices = wifiDevices;
            this.bluetoothDevices = bluetoothDevices;
            if (combinedDeviceList.size() > 0)
                combinedDeviceList.clear();
            combinedDeviceList.addAll(this.bluetoothDevices);
            combinedDeviceList.addAll(this.wifiDevices);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    deviceListAdapter.notifyDataSetChanged();
                }
            });
            if (willRecordRSSI) {
                if (Constants.EXP_NO == Constants.MAX_NO_OF_EXPS) {
                    showToast("rssi recorded");
                    willRecordRSSI = false;
                    final Button recordRSSI = findViewById(R.id.record_rssi_button);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            recordRSSI.setText("record rssi");
                        }
                    });
                    Constants.EXP_NO = 0;
                }
                else {
                    long currentTime = Calendar.getInstance().getTimeInMillis();
                    String timestamp = String.valueOf(currentTime);
                    FileWriter.writeRSSIResult(distance, timestamp, bluetoothDevices);
                    Constants.EXP_NO++;
                }
            }
        }
    }

    public ArrayList<Device> cleanUpDeviceList(ArrayList<Device> devices, int deviceType) {
        ArrayList<Device> cleanedList = new ArrayList<>();
        if (deviceType == Constants.WIFI_DEVICE) {
            for (Device newDevice: devices
                 ) {
                String deviceName = newDevice.wifiDevice.deviceName;
                if (deviceName!=null && newDevice.wifiDevice.deviceName.contains("NWSL")) {
                    boolean newDeviceFlag = true;
                    for (Device oldDevice: cleanedList
                            ) {
                        if (oldDevice.wifiDevice.deviceAddress.equals(newDevice.wifiDevice.deviceAddress)) {
                            newDeviceFlag = false;
                            break;
                        }
                    }
                    if (newDeviceFlag)
                        cleanedList.add(newDevice);
                }
            }
        }
        else {
            for (Device newDevice: devices
                    ) {
                String deviceName = newDevice.bluetoothDevice.getName();
                if (deviceName!=null && deviceName.contains("NWSL")) {
                    boolean newDeviceFlag = true;
                    for (Device oldDevice: cleanedList
                            ) {
                        if (oldDevice.bluetoothDevice.getAddress().equals(newDevice.bluetoothDevice.getAddress())) {
                            newDeviceFlag = false;
                            break;
                        }
                    }
                    if (newDeviceFlag)
                        cleanedList.add(newDevice);
                }
            }
        }
        return cleanedList;
    }

    //shows the wifi p2p state
    public void wifiP2PState(int state) {
        if (state == 0)
            showAlert("WiFi direct disabled");
    }

    public void connectionEstablished(int connectionType, BluetoothSocket connectedSocket) {
        if (connectionType == Constants.WIFI_DIRECT_CONNECTION) {
            showToast("wifi direct connection established");
            WDUDPListener udpListener = new WDUDPListener(this);
            udpListener.start();
//            if (!Constants.isGroupOwner)
//                ipMacSync();
        }
        else {
            showToast("bluetooth connection established");
            btConnectedSocketManager = new BTConnectedSocketManager(connectedSocket, this);
            btConnectedSocketManager.start();
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
                    device.lossRatioPktsReceived = 0;
                    willUpdateDeviceList = false;
                    showToast("ip mac synced");
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
                        if (experimentNo < Constants.MAX_NO_OF_EXPS) {
                            udpSender = null;
                            udpSender = new WDUDPSender();
                            udpSender.createPkt(device.rttPkt, srcAddr);
                            udpSender.setRunLoop(false);
                            device.rttStartTime = Calendar.getInstance().getTimeInMillis();
                            udpSender.start();
                        }
                        else {
                            writeResult(device.wifiDevice.deviceName, Constants.RTT, Constants.WIFI_DEVICE);
                        }
                        break;
                    }
                }
            }
        }
        else if (pktType == Constants.PKT_LOSS) {
            for (final Device device:combinedDeviceList
                 ) {
                if (device.deviceType == Constants.WIFI_DEVICE) {
                    if (device.wifiDevice.deviceAddress.equals(splited[1])) {
                        if (device.lossRatioPktsReceived == 0) {
                            device.lossRatioPktsReceived++;
                            Log.d("pkt loss", "first pkt");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Handler handler = new Handler();
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            double pktLoss = ((Constants.MAX_LOSS_RATIO_PKTS - device.lossRatioPktsReceived)/Constants.MAX_LOSS_RATIO_PKTS) * 100.00;
                                            Log.d("pkt loss in 5 seconds", String.valueOf(pktLoss));
                                        }
                                    }, 5 * 1000);
                                }
                            });
                        }
                        else
                            device.lossRatioPktsReceived++;
                    }
                }
            }
        }
        else if (pktType == Constants.UDP_THROUGHPUT) {
            int pktSize = Integer.parseInt(splited[3]);
            String pkt = PacketManager.createRTTPacket(Constants.UDP_THROUGHPUT_RET, Constants.hostWifiAddress, splited[1], pktSize);
            udpSender = null;
            udpSender = new WDUDPSender();
            udpSender.createPkt(pkt, srcAddr);
            udpSender.setRunLoop(false);
            udpSender.start();
        }
        else if (pktType == Constants.UDP_THROUGHPUT_RET) {
            for (Device device:combinedDeviceList
                    ) {
                if (device.deviceType == Constants.WIFI_DEVICE) {
                    if (device.wifiDevice.deviceAddress.equals(splited[1])) {
                        device.roundTripTime = receivingTime - device.rttStartTime;
                        udpThroughputRTTs[experimentNo] = device.roundTripTime;
                        experimentNo++;
                        if (experimentNo < Constants.MAX_NO_OF_EXPS) {
                            String packet = PacketManager.createRTTPacket(Constants.UDP_THROUGHPUT, Constants.hostWifiAddress, splited[1], udpThrpughputPktSizes[experimentNo]);
                            udpSender = null;
                            udpSender = new WDUDPSender();
                            udpSender.createPkt(packet, srcAddr);
                            udpSender.setRunLoop(false);
                            device.rttStartTime = Calendar.getInstance().getTimeInMillis();
                            udpSender.start();
                        }
                        else {
                            writeResult(device.wifiDevice.deviceName, Constants.UDP_THROUGHPUT, Constants.WIFI_DEVICE);
                        }
                        break;
                    }
                }
            }
        }
    }

    public void processReceivedBTPkt(byte[] readBuffer, long receivingTime, int numBytes) {
        final String receivedPkt = new String(readBuffer);
        String splited[] = receivedPkt.split("#");
        int pktType = Integer.parseInt(splited[0]);
        if (pktType == Constants.RTT) {
            for (Device device: combinedDeviceList) {
                if (device.deviceType == Constants.BLUETOOTH_DEVICE && device.bluetoothDevice.getName().equals(splited[1])) {
                    int pktSize = Integer.parseInt(splited[3]);
                    btConnectedSocketManager.setDevice(device);
                    String packet = PacketManager.createRTTPacket(Constants.RTT_RET, Constants.hostBluetoothName, splited[1], pktSize);
                    btConnectedSocketManager.sendPkt(packet, Constants.RTT_RET);
                    break;
                }
            }
        }
        else if (pktType == Constants.RTT_RET) {
            for (final Device device: combinedDeviceList) {
                if (device.deviceType == Constants.BLUETOOTH_DEVICE && device.bluetoothDevice.getName().equals(splited[1])) {
                    device.roundTripTime = receivingTime - device.rttStartTime;
                    RTTs[Constants.EXP_NO] = device.roundTripTime;
                    Constants.EXP_NO++;
                    if (Constants.EXP_NO == Constants.MAX_NO_OF_EXPS) {
                        writeResult(device.bluetoothDevice.getName(), Constants.RTT, Constants.BLUETOOTH_DEVICE);
                        Constants.EXP_NO = 0;
                    }
                    else {
                        calculateBTRTT(device, Integer.parseInt(splited[3]));
                    }
                }
            }
        }
    }

    public void  writeResult(String deviceName, int measurementType, int deviceType) {
        EditText distanceText = findViewById(R.id.distance_editText);
        String distance = distanceText.getText().toString().trim();

        EditText pktSizeText = findViewById(R.id.pkt_size_editText);
        String pktSize = pktSizeText.getText().toString().trim();

        if (measurementType == Constants.RTT) {
            boolean retVal = FileWriter.writeRTTResult(deviceName, pktSize, distance, RTTs, deviceType);
            if (retVal)
                showToast("rtt written successfully");
            else
                showToast("rtt write not successful");
        }
        else if (measurementType == Constants.UDP_THROUGHPUT) {
            boolean retVal = FileWriter.writeThroughputRTTs(deviceName, distance, udpThroughputRTTs);
            if (retVal)
                showToast("throughput rtt written successfully");
            else
                showToast("throughput rtt write not successful");
        }
    }

    //function to show an alert message
    public void showAlert(final String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
            }
        });
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