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
import java.util.Arrays;
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
    int currentSeqNo;
    long RTTs[];
    long rttToWrite[];
    boolean RTTCalculated[];
    int rttCalculatedCount;
    Handler rttHandler;
    Handler pktLossHandler;
    String distance;
    Device currentDevice;
    int currentPktSize;
    long initialStartTime;
    long cumulativeRTTs[];
    int pktReceiveCount[];
    boolean pktReceiveCounted[];
    boolean pktLossExpStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        willUpdateDeviceList = true;
        willRecordRSSI = false;
        setUpPermissions();
//        BTDiscoverableHandler = new Handler();
//        BTDiscoverableHandler.post(makeBluetoothDiscoverable);
//        setUpBluetoothDataTransfer();
        startDiscovery();
//        getBTPairedDevices();
        pktLossExpStarted = false;
        pktReceiveCount = new int[Constants.MAX_PKT_LOSS_EXPS];
        Arrays.fill(pktReceiveCount, 0);
        pktReceiveCounted = new boolean[Constants.MAX_PKT_LOSS_EXPS];
        Arrays.fill(pktReceiveCounted, false);
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
        EditText distanceText = findViewById(R.id.distance_editText);
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
        currentDevice = combinedDeviceList.get(tag);

        peerDiscoveryController.connectWiFiDirectDevice(combinedDeviceList.get(tag));
    }

    public void manageRttTimeBound(int seqNo) {
        if (!RTTCalculated[seqNo]) {
            currentSeqNo++;
            if (rttCalculatedCount < Constants.MAX_NO_OF_EXPS && currentSeqNo < 1000) {
                String rttPkt = PacketManager.createWDRTTPacket(Constants.RTT, currentSeqNo, Constants.hostWifiAddress, currentDevice.wifiDevice.deviceAddress, currentPktSize);
                sendWDRTTPkt(rttPkt, currentDevice.IPAddress);
            }
        }
    }

    public void managePktLossTimeBound() {
        Constants.EXP_NO++;
        if (Constants.EXP_NO < Constants.MAX_PKT_LOSS_EXPS) {
            startPktLossExp();
        }
    }

    public void rttButton(View view) {
        int tag = (int)view.getTag();
        currentDevice = combinedDeviceList.get(tag);
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
        String pktSizeStr = pktSizeText.getText().toString().trim();
        currentPktSize = Integer.parseInt(pktSizeStr);
        if (currentDevice.deviceType == Constants.WIFI_DEVICE) {
            if (currentDevice.IPAddress == null) {
                showToast("ip mac not synced");
                return;
            }
            calculateWDRTT(currentDevice, currentPktSize);
        }
        else {
            calculateBTRTT(currentDevice, currentPktSize);
        }
    }

    public void calculateBTRTT(Device currentDevice, int pktSize) {
        cumulativeRTTs = new long[Constants.MAX_NO_OF_EXPS];
        Constants.EXP_NO = 0;
        RTTs = new long[Constants.MAX_NO_OF_EXPS];
        Arrays.fill(RTTs, 0);
        BTSocketConnector socketConnector = new BTSocketConnector();
        socketConnector.setDevice(currentDevice);
        BluetoothSocket connectedSocket = socketConnector.createSocket();
        btConnectedSocketManager = null;
        if (connectedSocket!=null) {
            btConnectedSocketManager = new BTConnectedSocketManager(connectedSocket, this);
            btConnectedSocketManager.start();
        }
        btConnectedSocketManager.setDevice(currentDevice);
        String packet = PacketManager.createBluetoothRTTPacket(Constants.RTT, Constants.hostBluetoothName, currentDevice.bluetoothDevice.getName(), pktSize);
        RTTs[Constants.EXP_NO] = btConnectedSocketManager.sendPkt(packet);
    }

    public void calculateWDRTT(Device currentDevice, int pktSize) {
        rttToWrite = new long[Constants.MAX_NO_OF_EXPS];
        cumulativeRTTs = new long[Constants.MAX_NO_OF_EXPS];
        Constants.EXP_NO = 0;
        rttHandler = new Handler();
        currentSeqNo = 0;
        rttCalculatedCount = 0;
        RTTs = new long[1000];
        Arrays.fill(RTTs, 0);
        RTTCalculated = new boolean[1000];
        Arrays.fill(RTTCalculated, false);
        String rttPkt = PacketManager.createWDRTTPacket(Constants.RTT, currentSeqNo, Constants.hostWifiAddress, currentDevice.wifiDevice.deviceAddress, pktSize);
        sendWDRTTPkt(rttPkt, currentDevice.IPAddress);
    }

    public void sendWDRTTPkt(String pkt, InetAddress destinationIP) {
        udpSender = null;
        udpSender = new WDUDPSender();
        udpSender.setRunLoop(false);
        udpSender.createPkt(pkt, destinationIP);
        RTTs[currentSeqNo] = Calendar.getInstance().getTimeInMillis();
        if (currentSeqNo == 0)
            initialStartTime = RTTs[currentSeqNo];
        udpSender.start();
        rttHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                manageRttTimeBound(currentSeqNo);
            }
        }, 1000);
    }

    public void pktLossButton(View view) {
        int tag = (int)view.getTag();
        currentDevice = combinedDeviceList.get(tag);
        if (currentDevice.IPAddress == null) {
            showToast("ip mac not synced");
            return;
        }
        Constants.EXP_NO = 0;
        pktLossHandler = null;
        pktLossHandler = new Handler();
        showToast("pkt loss experiment started");
        startPktLossExp();
    }

    public void startPktLossExp() {
        udpSender = null;
        udpSender = new WDUDPSender();
        String lossRatioPkt = PacketManager.createLossRatioPacket(Constants.PKT_LOSS, Constants.EXP_NO, Constants.hostWifiAddress, currentDevice.wifiDevice.deviceAddress);
        udpSender.createPkt(lossRatioPkt, currentDevice.IPAddress);
        udpSender.setRunLoop(true);
        udpSender.setNoOfPktsToSend(Constants.MAX_LOSS_RATIO_PKTS);
        udpSender.start();
        pktLossHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                managePktLossTimeBound();
            }
        }, 5000);
    }

    public void UDPThroughputButton(View view) {
        int tag = (int)view.getTag();
        currentDevice = combinedDeviceList.get(tag);
        EditText distanceText = findViewById(R.id.distance_editText);
        if (textboxIsEmpty(distanceText)) {
            distanceText.setError("enter distance");
            return;
        }
        if (currentDevice.IPAddress == null) {
            showToast("ip mac not synced");
            return;
        }
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
                    Log.d("finished exp no", String.valueOf(Constants.EXP_NO));
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

    public void connectionEstablished(int connectionType, BluetoothSocket connectedSocket) {
        if (connectionType == Constants.WIFI_DIRECT_CONNECTION) {
            showToast("wifi direct connection established");
            WDUDPListener udpListener = new WDUDPListener(this);
            udpListener.start();
            if (!Constants.isGroupOwner)
                ipMacSync();
        }
        else {
            showToast("bluetooth connection established");
            btConnectedSocketManager = new BTConnectedSocketManager(connectedSocket, this);
            btConnectedSocketManager.start();
        }
    }

    public void ipMacSync() {
        String pkt = PacketManager.createIpMacSyncPkt(Constants.IP_MAC_SYNC, Constants.hostWifiAddress);
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
        if (pktType == Constants.IP_MAC_SYNC) {
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
            int pktSize = Integer.parseInt(splited[4]);
            int seqNo = Integer.parseInt(splited[1]);
            String pkt = PacketManager.createWDRTTPacket(Constants.RTT_RET, seqNo, Constants.hostWifiAddress, splited[2], pktSize);
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
                    if (device.wifiDevice.deviceAddress.equals(splited[2])) {
                        int seqNo = Integer.parseInt(splited[1]);
                        int pktSize = Integer.parseInt(splited[4]);
                        if (seqNo == currentSeqNo) {
                            RTTs[seqNo] = receivingTime - RTTs[seqNo];
                            rttToWrite[Constants.EXP_NO] = RTTs[seqNo];
                            cumulativeRTTs[Constants.EXP_NO] = receivingTime - initialStartTime;
                            Constants.EXP_NO++;
                            RTTCalculated[seqNo] = true;
                            rttCalculatedCount++;
                            currentSeqNo++;
                            if (rttCalculatedCount < Constants.MAX_NO_OF_EXPS && currentSeqNo < 1000) {
                                String rttPkt = PacketManager.createWDRTTPacket(Constants.RTT, currentSeqNo, Constants.hostWifiAddress, splited[2], pktSize);
                                sendWDRTTPkt(rttPkt, srcAddr);
                            } else {
                                Constants.EXP_NO = 0;
                                writeResult(device.wifiDevice.deviceName, Constants.RTT, Constants.WIFI_DEVICE);
                            }
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
                    if (device.wifiDevice.deviceAddress.equals(splited[2])) {
                        currentDevice = device;
                        final int expNo = Integer.parseInt(splited[1]);
                        if (!pktReceiveCounted[expNo]) {
                            if (pktReceiveCount[expNo] == 0) {
                                if (!pktLossExpStarted) {
                                    pktLossExpStarted = true;
                                    final Button rssi = findViewById(R.id.record_rssi_button);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            rssi.setText("pkt loss running");
                                        }
                                    });
                                }
                                pktReceiveCount[expNo]++;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Handler handler = new Handler();
                                        handler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                pktReceiveCounted[expNo] = true;
                                                Log.d(String.valueOf(expNo), String.valueOf(pktReceiveCount[expNo]));
                                                int expCounter = 0;
                                                for (int i=0; i<Constants.MAX_PKT_LOSS_EXPS; i++)
                                                    if (pktReceiveCounted[i])
                                                        expCounter++;
                                                if (expCounter == Constants.MAX_PKT_LOSS_EXPS) {
                                                    writeResult(currentDevice.wifiDevice.deviceName, Constants.PKT_LOSS, Constants.WIFI_DEVICE);
                                                    pktReceiveCount = new int[Constants.MAX_PKT_LOSS_EXPS];
                                                    Arrays.fill(pktReceiveCount, 0);
                                                    pktReceiveCounted = new boolean[Constants.MAX_PKT_LOSS_EXPS];
                                                    Arrays.fill(pktReceiveCounted, false);
                                                }
                                            }
                                        }, 2000);
                                    }
                                });
                            }
                            else
                                pktReceiveCount[expNo]++;
                        }
                    }
                }
            }
        }
        else if (pktType == Constants.UDP_THROUGHPUT) {
        }
        else if (pktType == Constants.UDP_THROUGHPUT_RET) {
            for (Device device:combinedDeviceList
                    ) {
                if (device.deviceType == Constants.WIFI_DEVICE) {
                    if (device.wifiDevice.deviceAddress.equals(splited[1])) {
                        break;
                    }
                }
            }
        }
    }

    public void processReceivedBTPkt(byte[] readBuffer, long receivingTime) {
        final String receivedPkt = new String(readBuffer);
        String splited[] = receivedPkt.split("#");
        int pktType = Integer.parseInt(splited[0]);
        if (pktType == Constants.RTT) {
            for (Device device: combinedDeviceList) {
                if (device.deviceType == Constants.BLUETOOTH_DEVICE && device.bluetoothDevice.getName().equals(splited[1])) {
                    int pktSize = Integer.parseInt(splited[3]);
                    btConnectedSocketManager.setDevice(device);
                    String packet = PacketManager.createBluetoothRTTPacket(Constants.RTT_RET, Constants.hostBluetoothName, splited[1], pktSize);
                    btConnectedSocketManager.sendPkt(packet);
                    break;
                }
            }
        }
        else if (pktType == Constants.RTT_RET) {
            for (final Device device: combinedDeviceList) {
                if (device.deviceType == Constants.BLUETOOTH_DEVICE && device.bluetoothDevice.getName().equals(splited[1])) {
                    RTTs[Constants.EXP_NO] = receivingTime - RTTs[Constants.EXP_NO];
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
            boolean retVal;
            if (deviceType == Constants.WIFI_DEVICE)
                retVal = FileWriter.writeRTTResult(deviceName, pktSize, distance, rttToWrite, deviceType, cumulativeRTTs);
            else
                retVal = FileWriter.writeRTTResult(deviceName, pktSize, distance, RTTs, deviceType, cumulativeRTTs);
            if (retVal)
                showToast("rtt written successfully");
            else
                showToast("rtt write not successful");
        }
        else if (measurementType == Constants.PKT_LOSS) {
            boolean retVal = FileWriter.writePktLossResult(deviceName, distance, pktReceiveCount);
            pktLossExpStarted = false;
            final Button rssi = findViewById(R.id.record_rssi_button);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    rssi.setText("pkt loss stopped");
                }
            });
            if (retVal)
                showToast("pkt loss result written successfully");
            else
                showToast("pkt loss result writing not successful");
        }
//        else if (measurementType == Constants.UDP_THROUGHPUT) {
//            boolean retVal = FileWriter.writeThroughputRTTs(deviceName, distance, udpThroughputRTTs, );
//            if (retVal)
//                showToast("throughput rtt written successfully");
//            else
//                showToast("throughput rtt write not successful");
//        }
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