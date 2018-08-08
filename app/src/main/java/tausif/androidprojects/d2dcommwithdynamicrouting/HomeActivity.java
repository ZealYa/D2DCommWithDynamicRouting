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

    private static final int PICKFILE_REQUEST_CODE = 323;
    ArrayList<Device> combinedDeviceList;
    ArrayList<Device> wifiDevices;
    ArrayList<Device> bluetoothDevices;
    ListView deviceListView;
    DeviceListAdapter deviceListAdapter;
    private TransferService transferService;
    String NAME = "server";
    String MY_UUID = "e439084f-b7f1-460c-8a3f-d4cc883413e2";
    BluetoothAdapter bluetoothAdapter;
    BluetoothServerThread bluetoothServerThread;
    BluetoothDataSender bluetoothDataSender;
    ConnectedThread connectedThread;
    int runNo;
    long[] rttTimes;
    String rttPkt;
    String measuredDeviceName;
    PeerDiscoveryController peerDiscoveryController;
    WiFiDirectUDPSender udpSender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        transferService = new TransferService(this);
        configureBluetoothDataTransfer();
        startDiscovery();
    }

    //setting up the device list view adapter and item click events
    public void configureDeviceListView(){
        deviceListView = findViewById(R.id.device_listView);
        combinedDeviceList = new ArrayList<>();
        deviceListAdapter = new DeviceListAdapter(this, combinedDeviceList);
        deviceListView.setAdapter(deviceListAdapter);
    }

    //configures the bluetooth and wifi discovery options and starts the background process for discovery
    public void startDiscovery(){
        configureDeviceListView();
        peerDiscoveryController = new PeerDiscoveryController(this, this);
    }

    public void connectButton(View view) {
        int tag = (int)view.getTag();
        Device currentDevice = combinedDeviceList.get(tag);
        if (currentDevice.deviceType == Constants.WIFI_DEVICE)
            peerDiscoveryController.connectWiFiDirectDevice(combinedDeviceList.get(tag));
    }

    public void connectionEstablished(int connectionType) {
        if (connectionType == Constants.WIFI_DIRECT_CONNECTION) {
            WiFiDirectUDPListener udpListener = new WiFiDirectUDPListener(this);
            udpListener.start();
            if (Constants.isGroupOwner)
//                Toast.makeText(this, "group owner", Toast.LENGTH_LONG).show();
            else {
//                Toast.makeText(this, "client", Toast.LENGTH_LONG).show();
                ipMacSync();
            }
        }
    }

    public void ipMacSync() {
//        Toast.makeText(this, "inside ip mac sync method", Toast.LENGTH_LONG).show();
        String pkt = PacketManager.createIpMacSyncPkt(Constants.IP_MAC_SYNC, Constants.hostWifiAddress);
        udpSender = new WiFiDirectUDPSender();
        udpSender.createPkt(pkt, Constants.groupOwnerAddress);
        udpSender.start();
    }

    public void rttButton(View view) {
        int tag = (int)view.getTag();
        Device currentDevice = combinedDeviceList.get(tag);
        if (currentDevice.deviceType == Constants.BLUETOOTH_DEVICE)
            measureBluetoothRTT();
        else {
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

    public void configureBluetoothDataTransfer() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Constants.hostBluetoothAddress = bluetoothAdapter.getAddress();
        bluetoothServerThread = new BluetoothServerThread();
        bluetoothServerThread.start();
        bluetoothDataSender = new BluetoothDataSender();
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

    public void writeRTTResult() {
        EditText distanceText = findViewById(R.id.distance_editText);
        String distance = distanceText.getText().toString().trim();
        final String filename = "RTT_" + measuredDeviceName + "_" + distance + "_meters.txt";
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showAlert(filename);
            }
        });
        File RTTResults = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), filename);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(RTTResults);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            for (int i=0;i<Constants.noOfRuns;i++) {
                outputStreamWriter.append(String.valueOf(rttTimes[i]));
                outputStreamWriter.append("\n");
                if (i == 19 || i == 39)
                    outputStreamWriter.append("\n");
            }
            outputStreamWriter.close();
            fileOutputStream.close();
        } catch (IOException FIOExec) {

        }
    }

    public void measureBluetoothRTT() {
        for (Device device: bluetoothDevices
             ) {
            String deviceName = device.bluetoothDevice.getName();
            if (deviceName != null && deviceName.contains("NWSL")) {
                measuredDeviceName = deviceName;
                runNo=0;
                rttTimes = new long[Constants.noOfRuns];
                rttPkt = PacketManager.createRTTPacket(Constants.RTT, Constants.hostBluetoothAddress, device.bluetoothDevice.getAddress(), Constants.RTT_PKT_SIZE);
                bluetoothDataSender.setDevice(device);
                bluetoothDataSender.createSocket();
                bluetoothDataSender.sendPkt(rttPkt, Constants.RTT);
            }
        }
    }

    private boolean hasStorageWriteAccess() {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1415);
            return false;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        transferService.shutdown();
    }

    //function to show an alert message
    public void showAlert(String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    //shows the wifi p2p state
    public void wifiP2PState(int state) {
        if (state == 0)
            showAlert("WiFi p2p disabled");
    }

    public void manageConnectedBluetoothSocket(BluetoothSocket socket){
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();
    }

    //bluetooth server thread
    private class BluetoothServerThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public BluetoothServerThread() {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, UUID.fromString(MY_UUID));
            } catch (IOException e) {
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }

                if (socket != null) {
                    manageConnectedBluetoothSocket(socket);
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
            }
        }
    }
    //end of bluetooth server thread

    private class BluetoothDataSender {
        private BluetoothSocket socket;
        private Device device;

        public void setDevice(Device device) {
            this.device = device;
        }

        public void setSocket(BluetoothSocket socket) {
            this.socket = socket;
        }

        public void createSocket() {
            try {
                socket = device.bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));
            }catch (IOException sktCrt) {

            }
            try {
                socket.connect();
            }catch (IOException sktCnct) {
                try {
                    socket.close();
                }catch (IOException sktClse) {

                }
            }
            manageConnectedBluetoothSocket(socket);
        }
        public void sendPkt(String packet, int pktType) {
            try {
                OutputStream outputStream = socket.getOutputStream();
                if (pktType == Constants.RTT)
                    device.rttStartTime = Calendar.getInstance().getTimeInMillis();
                outputStream.write(packet.getBytes());
                outputStream.flush();
            } catch (IOException writeEx) {

            }
        }
    }

    //bluetooth data send/receive thread
    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private byte[] readBuffer; // mmBuffer store for the stream

        public ConnectedThread(BluetoothSocket socket) {
            this.socket = socket;
            InputStream tmpIn = null;
            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = this.socket.getInputStream();
            } catch (IOException e) {
                Log.e("input stream error", "Error occurred when creating input stream", e);
            }
            inputStream = tmpIn;
        }

        public void run() {
            readBuffer = new byte[1500];
            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
                    numBytes = inputStream.read(readBuffer);
                    long receiveTime = Calendar.getInstance().getTimeInMillis();
                    processReceivedBTPkt(readBuffer, receiveTime, numBytes);
                } catch (IOException e) {
                    Log.d("disconnection error", "Input stream was disconnected", e);
                    break;
                }
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e("socket closing error", "Could not close the connect socket", e);
            }
        }
    }

    public void processReceivedWiFiPkt(InetAddress srcAddr, final String receivedPkt) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showAlert(receivedPkt);
            }
        });
    }

    public void processReceivedBTPkt(byte[] receivedData, long receiveTime, int numBytesRead) {
        final String receivedPkt = new String(receivedData);
        String splited[] = receivedPkt.split("#");
        int pktType = Integer.parseInt(splited[0]);
        if (pktType == Constants.RTT) {
            for (Device device: bluetoothDevices
                    ) {
                if (device.bluetoothDevice.getAddress().equals(splited[1])) {
                    bluetoothDataSender.setDevice(device);
                    break;
                }
            }
            Constants.RTT_PKT_SIZE = Integer.parseInt(splited[3]);
            Log.d("pkt size",String.valueOf(Constants.RTT_PKT_SIZE));
            String packet = PacketManager.createRTTPacket(Constants.RTT_RET, splited[2], splited[1], Constants.RTT_PKT_SIZE);
            bluetoothDataSender.setSocket(connectedThread.socket);
            bluetoothDataSender.sendPkt(packet, Constants.RTT_RET);
        }
        else if (pktType == Constants.RTT_RET) {
            for (Device device: bluetoothDevices
                 ) {
                if (device.bluetoothDevice.getAddress().equals(splited[1])) {
                    device.roundTripTime = receiveTime - device.rttStartTime;
                    rttTimes[runNo] = device.roundTripTime;
                    runNo++;
                    if (runNo < Constants.noOfRuns){
                        if (runNo == 20 || runNo == 40) {
                            Constants.RTT_PKT_SIZE *=2;
                            rttPkt = PacketManager.createRTTPacket(Constants.RTT, Constants.hostBluetoothAddress, device.bluetoothDevice.getAddress(), Constants.RTT_PKT_SIZE);
                        }
                        device.rttStartTime = 0;
                        device.rttEndTime = 0;
                        device.roundTripTime = 0;
                        bluetoothDataSender.sendPkt(rttPkt, Constants.RTT);
                    }
                    else {
                        writeRTTResult();
                    }
                    break;
                }
            }
        }
    }

    public void onWifiP2PDeviceConnected(final WifiP2pInfo wifiInfo, final WifiP2pDevice device) {
//      Log.d("connected device", device.deviceName);
//        if(wifiInfo.isGroupOwner){
//            transferService.startServer(8089);
//        }
//        else{
//            //wait 1/2 second for server
//            Toast.makeText(this,"Waiting for server ", Toast.LENGTH_SHORT).show();
//            handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    transferService.establishConnection(wifiInfo.groupOwnerAddress.getHostAddress(),8089);
//                }
//            },500);
//
//        }
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == PICKFILE_REQUEST_CODE && resultCode == RESULT_OK){
            Log.e("file path",data.getDataString());
        }
        if (resultCode == RESULT_CANCELED)
            showAlert("Bluetooth can not be enabled");
        super.onActivityResult(requestCode, resultCode, data);
    }
}