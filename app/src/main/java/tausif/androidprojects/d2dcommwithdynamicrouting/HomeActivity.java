package tausif.androidprojects.d2dcommwithdynamicrouting;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import tausif.androidprojects.files.TransferService;

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
    int currentSeqNo;
    long RTTs[];
    long rttToWrite[];
    boolean RTTCalculated[];
    int rttCalculatedCount;
    Handler rttHandler;
    Handler pktLossHandler;
    Device currentDevice;
    int currentPktSize;
    long initialStartTime;
    long cumulativeRTTs[];
    int correspondingPktSize[];
    int pktReceiveCount[];
    boolean pktReceiveCounted[];
    boolean pktLossExpStarted;
    TransferService transferService;
    Handler handler;
    boolean rssiRecorded;
    ArrayList<Device> rssiDevices;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        initData();
        initOperations();
    }

    public void initData() {
        willUpdateDeviceList = false;
        pktLossExpStarted = false;
        pktReceiveCount = new int[Constants.MAX_PKT_LOSS_EXPS];
        Arrays.fill(pktReceiveCount, 0);
        pktReceiveCounted = new boolean[Constants.MAX_PKT_LOSS_EXPS];
        Arrays.fill(pktReceiveCounted, false);
        rssiRecorded = false;
        rssiDevices = new ArrayList<>();
        Constants.EXP_NO = 0;
        configureDeviceListView();
    }

    public void initOperations() {
        setUpPermissions();
        BTDiscoverableHandler = new Handler();
        BTDiscoverableHandler.post(makeBluetoothDiscoverable);
        handler = new Handler();
        transferService = new TransferService(this, this);
        setUpBluetoothDataTransfer();
    }

    public void setUpPermissions() {
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constants.REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION);
        }
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, Constants.REQUEST_CODE_LOCATION);
        File resultFolder = new File(Environment.getExternalStorageDirectory() + "/" + Constants.RESULT_FOLDER_NAME);
        if (resultFolder.exists())
            showToast("result folder exists", 1);
        else {
            showToast("creating result folder", 1);
            boolean folderCreated = resultFolder.mkdir();
            if (folderCreated)
                showToast("result folder created successfully", 1);
            else
                showToast("could not create result folder", 1);
        }
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
    public void startDiscovery(View view){
        willUpdateDeviceList = true;
        peerDiscoveryController = new PeerDiscoveryController(this, this);
    }

    //setting up the device list view adapter and item click events
    public void configureDeviceListView(){
        deviceListView = findViewById(R.id.device_listView);
        combinedDeviceList = new ArrayList<>();
        deviceListAdapter = new DeviceListAdapter(this, combinedDeviceList);
        deviceListView.setAdapter(deviceListAdapter);
    }

    //callback method from peer discovery controller after finishing a cycle of wifi and bluetooth discovery
    public void discoveryFinished(ArrayList<Device> wifiDevices, ArrayList<Device> bluetoothDevices) {
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
            if (!rssiRecorded) {
                if (Constants.EXP_NO == Constants.MAX_NO_OF_EXPS) {
                    showToast("rssi recorded", 1);
                    Constants.EXP_NO = 0;
                    rssiRecorded = true;
                    writeResult(null, Constants.RSSI, Constants.BLUETOOTH_DEVICE);
                }
                else {
                    rssiDevices.addAll(this.bluetoothDevices);
                    Constants.EXP_NO++;
                }
            }
        }
    }

    public void connectBTButton(View view) {
        int tag = (int)view.getTag();
        Device currentDevice = combinedDeviceList.get(tag);
        currentDevice.bluetoothDevice.createBond();
    }

    public void connectWDButton(View view) {
        int tag = (int)view.getTag();
        currentDevice = combinedDeviceList.get(tag);
        peerDiscoveryController.connectWiFiDirectDevice(combinedDeviceList.get(tag));
    }

    public void connectionEstablished(int connectionType, BluetoothSocket connectedSocket) {
        if (connectionType == Constants.WIFI_DIRECT_CONNECTION) {
            showToast("wifi direct connection established", 1);
            if (Constants.isGroupOwner)
                transferService.startServer(8089);
            else {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        transferService.establishConnection(Constants.groupOwnerAddress.getHostAddress(), 8089);
                    }
                }, 3000);
            }
            WDUDPListener udpListener = new WDUDPListener(this);
            udpListener.start();
            if (!Constants.isGroupOwner)
                ipMacSync();
        }
        else {
            showToast("bluetooth connection established", 1);
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
                    showToast("ip mac synced", 1);
                    break;
                }
            }
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
        if (currentDevice.deviceType == Constants.WIFI_DEVICE) {
            if (currentDevice.IPAddress == null) {
                showToast("ip mac not synced", 1);
                return;
            }
            willUpdateDeviceList = false;
            currentPktSize = Constants.RTT_PKT_SIZE;
            calculateWDRTT(currentDevice, Constants.RTT_PKT_SIZE);
        }
        else {
            willUpdateDeviceList = false;
            calculateBTRTT();
        }
    }

    public void calculateBTRTT() {
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
            String packet = PacketManager.createBluetoothRTTPacket(Constants.RTT, Constants.hostBluetoothName, currentDevice.bluetoothDevice.getName(), Constants.RTT_PKT_SIZE);
            RTTs[Constants.EXP_NO] = btConnectedSocketManager.sendPkt(packet);
        }
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
        correspondingPktSize = new int[1000];
        correspondingPktSize[currentSeqNo] = pktSize;
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

    public void manageRttTimeBound(int seqNo) {
        if (!RTTCalculated[seqNo]) {
            currentSeqNo++;
            if (rttCalculatedCount < Constants.MAX_NO_OF_EXPS && currentSeqNo < 1000) {
//                currentPktSize += 5;
                String rttPkt = PacketManager.createWDRTTPacket(Constants.RTT, currentSeqNo, Constants.hostWifiAddress, currentDevice.wifiDevice.deviceAddress, currentPktSize);
                sendWDRTTPkt(rttPkt, currentDevice.IPAddress);
            }
        }
    }

    public void pktLossButton(View view) {
        int tag = (int)view.getTag();
        currentDevice = combinedDeviceList.get(tag);
        if (currentDevice.IPAddress == null) {
            showToast("ip mac not synced", 1);
            return;
        }
        Constants.EXP_NO = 0;
        pktLossHandler = null;
        pktLossHandler = new Handler();
        showToast("pkt loss experiment started", 1);
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
        }, 250);
    }

    public void managePktLossTimeBound() {
        Constants.EXP_NO++;
        if (Constants.EXP_NO < Constants.MAX_PKT_LOSS_EXPS) {
            startPktLossExp();
        }
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
            showToast("ip mac not synced", 1);
            return;
        }
        currentPktSize = 100;
        calculateWDRTT(currentDevice, currentPktSize);
    }

    public void TCPThroughputButton(View view) {
        transferService.sendFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath(), "test.txt");
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
                            correspondingPktSize[seqNo] = pktSize;
//                            currentPktSize = pktSize + 5;
                            cumulativeRTTs[Constants.EXP_NO] = receivingTime - initialStartTime;
                            Constants.EXP_NO++;
                            RTTCalculated[seqNo] = true;
                            rttCalculatedCount++;
                            currentSeqNo++;
                            if (rttCalculatedCount < Constants.MAX_NO_OF_EXPS && currentSeqNo < 1000) {
                                String rttPkt = PacketManager.createWDRTTPacket(Constants.RTT, currentSeqNo, Constants.hostWifiAddress, splited[2], currentPktSize);
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
            Log.d("loss pkt", receivedPkt);
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
                                                showToast("completed pkt loss exp no "+String.valueOf(expNo), 0);
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
                                                    pktLossExpStarted = false;
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
        else if (pktType == Constants.UDP_THRPT) {
        }
        else if (pktType == Constants.UDP_THRPT_RET) {
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
                    String packet = PacketManager.createBluetoothRTTPacket(Constants.RTT_RET, Constants.hostBluetoothName, splited[1], Constants.RTT_PKT_SIZE);
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
                        willUpdateDeviceList = true;
                        writeResult(device.bluetoothDevice.getName(), Constants.RTT, Constants.BLUETOOTH_DEVICE);
                        Constants.EXP_NO = 0;
                    }
                    else {
                        String packet = PacketManager.createBluetoothRTTPacket(Constants.RTT, Constants.hostBluetoothName, splited[1], Constants.RTT_PKT_SIZE);
                        RTTs[Constants.EXP_NO] = btConnectedSocketManager.sendPkt(packet);
                    }
                }
            }
        }
    }

    public void fileTransferFinished(int filesize, long totalTime, double throughput) {
        EditText distanceText = findViewById(R.id.distance_editText);
        String distance = distanceText.getText().toString().trim();

//        EditText deviceNameText = findViewById(R.id.pkt_size_editText);
//        String deviceName = deviceNameText.getText().toString().trim();
//        deviceName = "NWSL " + deviceName;
//        boolean retVal = FileWriter.writeTCPThroughput(deviceName, filesize, totalTime, throughput, distance);
//        if (retVal)
//            showToast("tcp throughput written");
//        else
//            showToast("tcp throughput writing not successful");
    }

    public void  writeResult(String deviceName, int measurementType, int deviceType) {
        EditText distanceText = findViewById(R.id.distance_editText);
        String distance = distanceText.getText().toString().trim();
        boolean writeSuccess = false;
        if (measurementType == Constants.RSSI) {
            writeSuccess = FileWriter.writeRSSIResult(distance, rssiDevices);
            if (writeSuccess)
                showToast("RSSI result written successfully", 1);
            else
                showToast("RSSI result write failed", 1);
        }
        else if (measurementType == Constants.RTT) {
            if (deviceType == Constants.BLUETOOTH_DEVICE)
                writeSuccess = FileWriter.writeRTTResult(deviceName, distance, RTTs, deviceType, cumulativeRTTs);
            else {
                writeSuccess = FileWriter.writeRTTResult(deviceName, distance, rttToWrite, deviceType, cumulativeRTTs);
            }
            if (writeSuccess)
                showToast("RTT result written successfully", 1);
            else
                showToast("RTT result write failed", 1);
        }
        else if (measurementType == Constants.PKT_LOSS) {
            writeSuccess = FileWriter.writePktLossResult(deviceName, distance, pktReceiveCount);
            pktLossExpStarted = false;
            if (writeSuccess)
                showToast("pkt loss result written successfully", 1);
            else
                showToast("pkt loss result writing not successful", 1);
        }
//                if (measurementType == Constants.RTT) {
//            boolean retVal;
//            if (deviceType == Constants.WIFI_DEVICE)
//                retVal = FileWriter.writeThroughputRTTs(deviceName, distance, rttToWrite, correspondingPktSize, cumulativeRTTs);
//            else
//                retVal = FileWriter.writeRTTResult(deviceName, pktSize, distance, RTTs, deviceType, cumulativeRTTs);
//            if (retVal)
//                showToast("thrpt rtt written successfully");
//            else
//                showToast("thrpt rtt write not successful");
//        }
//        else if (measurementType == Constants.UDP_THRPT) {
//            boolean retVal = FileWriter.writeThroughputRTTs(deviceName, distance, udpThroughputRTTs, );
//            if (retVal)
//                showToast("throughput rtt written successfully");
//            else
//                showToast("throughput rtt write not successful");
//        }
    }

//    function to show a long Toast message
    public void showToast(final String message, final int length) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), message, length).show();
            }
        });
    }

//    function to check empty text field
    public boolean textboxIsEmpty(EditText editText) {
        return editText.getText().toString().trim().length() == 0;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        peerDiscoveryController.stopPeerDiscovery();
        transferService.shutdown();
    }
}