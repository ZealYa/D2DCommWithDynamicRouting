package tausif.androidprojects.d2dcommwithdynamicrouting;

import android.Manifest;
import android.app.usage.ExternalStorageStats;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Environment;
import android.os.Handler;
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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import tausif.androidprojects.files.TransferService;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int PICKFILE_REQUEST_CODE = 323;
    ArrayList<Device> combinedDeviceList;
    ArrayList<Device> wifiDevices;
    ArrayList<Device> bluetoothDevices;
    ListView deviceListView;
    DeviceListAdapter deviceListAdapter;
    private TransferService transferService;
    private Handler handler;
    String NAME = "server";
    String MY_UUID = "e439084f-b7f1-460c-8a3f-d4cc883413e2";
    BluetoothAdapter bluetoothAdapter;
    BluetoothServerThread bluetoothServerThread;
    BluetoothDataSender bluetoothDataSender;
    ConnectedThread connectedThread;
    int metricToMeasure;
    File resultRSSI;
    private String hostName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        handler = new Handler();
        transferService = new TransferService(this);
//        findViewById(R.id.send_with_wifi_button).setOnClickListener(this);
        hasStorageWriteAccess();
        metricToMeasure = -1;
    }

    public void bluetoothRSSIButton(View view) {
        metricToMeasure = Constants.BT_RSSI;
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1415);
        }
        else {
            resultRSSI = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "resultRSSI.txt");
        }
        startDiscovery();
    }

    public void bluetoothRTTButton(View view) {
        metricToMeasure = Constants.BT_RTT;
        configureBluetoothDataTransfer();
        getBTPairedDevices();
//        startDiscovery();
    }

    public void getBTPairedDevices() {
        bluetoothDevices = new ArrayList<>();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device: pairedDevices
                 ) {
                Device newDevice = new Device(Constants.BLUETOOTH_DEVICE, null, device, 0);
                bluetoothDevices.add(newDevice);
            }
            combinedDeviceList.addAll(bluetoothDevices);
            deviceListAdapter.notifyDataSetChanged();
            if (hostName.equals("NWSL 1"))
                measureBluetoothRTT();
        }
    }

    public void bluetoothPktLossButton(View view) {
    }

    public void wifiRTTButton(View view) {
    }

    public void wifiPktLossButton(View view) {
    }

    public void startDiscoveryButton(View view) {
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
        PeerDiscoveryController peerDiscoveryController = new PeerDiscoveryController(this, this);
    }

    public void configureBluetoothDataTransfer() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        hostName = bluetoothAdapter.getName();
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
        Device dummyWifiDevice = new Device(Constants.WIFI_DEVICE, null, null, 0);
        combinedDeviceList.add(dummyWifiDevice);
        combinedDeviceList.addAll(this.wifiDevices);
        Device dummyBluetoothDevice = new Device(Constants.BLUETOOTH_DEVICE, null, null, 0);
        combinedDeviceList.add(dummyBluetoothDevice);
        combinedDeviceList.addAll(this.bluetoothDevices);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                deviceListAdapter.notifyDataSetChanged();
            }
        });
        if (metricToMeasure == Constants.BT_RSSI)
            measureBluetoothRSSI();
        else if (metricToMeasure == Constants.BT_RTT && hostName.equals("NWSL 1") && Constants.timeSlotCount == 1)
            measureBluetoothRTT();
    }

    public void measureBluetoothRSSI() {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(resultRSSI);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            for (Device device:combinedDeviceList
                 ) {
                if (device.deviceType == Constants.BLUETOOTH_DEVICE && device.bluetoothDevice != null && device.bluetoothDevice.getName().contains("NWSL")) {
                    outputStreamWriter.append(device.bluetoothDevice.getName() + " " + String.valueOf(device.rssi) + "\n");
                }
            }
            outputStreamWriter.append("\n");
            outputStreamWriter.close();
            fileOutputStream.close();
        } catch (IOException e) {

        }
    }

    public void measureBluetoothRTT() {
        for (Device device: bluetoothDevices
             ) {
            String deviceName = device.bluetoothDevice.getName();
            if (deviceName != null && deviceName.contains("NWSL")) {
                String packet = PacketManager.createRTTPacket(Constants.TYPE_RTT, Constants.hostBluetoothAddress, device.bluetoothDevice.getAddress());
                bluetoothDataSender.setDevice(device.bluetoothDevice);
                bluetoothDataSender.setSocket();
                device.rttStartTime = Calendar.getInstance().getTimeInMillis();
                bluetoothDataSender.sendPkt(packet);
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

    public void showReceivedData(byte[] receivedData){
        final String receivedString = new String(receivedData);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showAlert(receivedString);
            }
        });
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
//                    try {
                        // A connection was accepted. Perform work associated with
                        // the connection in a separate thread.
                        manageConnectedBluetoothSocket(socket);
//                        mmServerSocket.close();
//                        break;
//                    } catch (IOException e) {
//
//                    }
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
        private BluetoothDevice device;

        public void setDevice(BluetoothDevice device) {
            this.device = device;
        }
        public void setSocket() {
            try {
                socket = device.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));
            } catch (IOException ex) {

            }
        }
        public void sendPkt(String packet) {
            try {
                socket.connect();
            } catch (IOException connectEx) {
                try {
                    socket.close();
                } catch (IOException closeEx) {

                }
            }
            try {
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(packet.getBytes());
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
            readBuffer = new byte[2000];
//            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
//                    numBytes = mmInStream.read(mmBuffer);
                    inputStream.read(readBuffer);
                    processReceivedBTPkt(readBuffer);
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

    public void processReceivedBTPkt(byte[] receivedData) {
        final String receivedPkt = new String(receivedData);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showAlert(receivedPkt);
            }
        });
        String splited[] = receivedPkt.split(" ");
        int pktType = Integer.parseInt(splited[0]);
        if (pktType == Constants.TYPE_RTT) {
            for (Device device: bluetoothDevices
                    ) {
                if (device.bluetoothDevice.getAddress().equals(splited[1])) {
                    bluetoothDataSender.setDevice(device.bluetoothDevice);
                    break;
                }
            }
            bluetoothDataSender.setSocket();
            String packet = PacketManager.createRTTPacket(Constants.TYPE_RTT_RET, splited[2], splited[1]);
//            bluetoothDataSender.sendPkt(packet);
        }
        else if (pktType == Constants.TYPE_RTT_RET) {
            for (final Device device: bluetoothDevices
                 ) {
                if (device.bluetoothDevice.getAddress().equals(splited[1])) {
                    device.rttEndTime = Calendar.getInstance().getTimeInMillis();
                    device.roundTripTime = device.rttEndTime - device.rttStartTime;
                    Log.d("rtt "+device.bluetoothDevice.getName(), String.valueOf(device.roundTripTime));
                }
            }
        }
    }

    public void onWifiP2PDeviceConnected(final WifiP2pInfo wifiInfo) {
        if(wifiInfo.isGroupOwner){
            transferService.startServer(8089);
        }
        else{
            //wait 1/2 second for server
            Toast.makeText(this,"Waiting for server ", Toast.LENGTH_SHORT).show();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    transferService.establishConnection(wifiInfo.groupOwnerAddress.getHostAddress(),8089);
                }
            },500);

        }
    }

    @Override
    public void onClick(View view) {
//        if(view.getId() == R.id.send_with_wifi_button){
//            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//            intent.addCategory(Intent.CATEGORY_OPENABLE);
//            intent.setType("*/*");
//            startActivityForResult(intent, PICKFILE_REQUEST_CODE);
//            sending hard code file
//            EditText inputTextBox = (EditText)findViewById(R.id.input_editText);
//            textToSend = inputTextBox.getText().toString();
//            String filename = textToSend + ".txt";
//            File fileToSend = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), filename);
//            try {
//                FileOutputStream fileOutputStream = new FileOutputStream(fileToSend);
//                fileOutputStream.write(textToSend.getBytes());
//                fileOutputStream.close();
//            } catch (IOException e) {
//            }
//            transferService.sendFile(Environment.getExternalStorageDirectory().getAbsolutePath(),filename);
//            Toast.makeText(this,"Sending file...",Toast.LENGTH_SHORT).show();
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