package tausif.androidprojects.d2dcommwithdynamicrouting;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import tausif.androidprojects.files.TransferService;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int PICKFILE_REQUEST_CODE = 323;
    WifiP2pManager wifiP2pManager;
    WifiP2pManager.Channel channel;
    BroadcastReceiver broadcastReceiver;
    IntentFilter intentFilter;
    ArrayList<Device> wifiDevices;
    ArrayList<Device> bluetoothDevices;
    ArrayList<Device> combinedDeviceList;
    ListView deviceListView;
    DeviceListAdapter deviceListAdapter;
    int DEVICE_TYPE_WIFI = 0;
    int TYPE_SERVER = 0;
    int TYPE_CLIENT = 1;
    private TransferService transferService;
    private Handler handler;
    String NAME = "server";
    String MY_UUID = "e439084f-b7f1-460c-8a3f-d4cc883413e2";
    ConnectedThread connectedThread;
    BluetoothServerThread serverThread;
    String textToSend = "";
    BluetoothAdapter bluetoothAdapter;
    int timeSlotCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        handler = new Handler();
        transferService = new TransferService(this);
        findViewById(R.id.send_with_wifi_button).setOnClickListener(this);
        hasStorageWriteAccess();
    }

    //configures the bluetooth and wifi discovery options and starts the background process for discovery
    public void startDiscovery(View view){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        configureDeviceListView();
        intentFilter = new IntentFilter();
        configureWiFiDiscovery();
        configureBluetoothDiscovery();
        registerReceiver(broadcastReceiver, intentFilter);
        timeSlotCount = 0;
        int timeInterval = 15;
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new controlPeerDiscovery(), 0, timeInterval*1000);
    }

    //configure wifi device discovery options
    public void configureWiFiDiscovery() {
        wifiP2pManager = (WifiP2pManager)getSystemService(Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(this, getMainLooper(), null);
        broadcastReceiver = new PeerDiscoveryBroadcastReceiver(wifiP2pManager, channel, this);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    public void configureBluetoothDiscovery() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
    }

    public class controlPeerDiscovery extends TimerTask {
        @Override
        public void run() {
            Log.d("time slot count", String.valueOf(timeSlotCount));
            if (timeSlotCount%2==0){
                wifiDevices = new ArrayList<>();
                wifiP2pManager.discoverPeers(channel,null);
                bluetoothDevices = new ArrayList<>();
                bluetoothAdapter.startDiscovery();
            }
            else {
                wifiP2pManager.stopPeerDiscovery(channel, null);
                bluetoothAdapter.cancelDiscovery();
                peerDiscoveryFinished();
            }
            timeSlotCount++;
        }
    }

    public void peerDiscoveryFinished() {
        if (combinedDeviceList.size()>0)
            combinedDeviceList.clear();
        combinedDeviceList.add(new Device("WiFi Devices", "", 0, -1, null));
        combinedDeviceList.addAll(wifiDevices);
        combinedDeviceList.add(new Device("Bluetooth Devices", "", 0, -1, null));
        combinedDeviceList.addAll(bluetoothDevices);
        for (Device device: combinedDeviceList
             ) {
            if (device.deviceName == null)
                device.deviceName = "No name";
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                deviceListAdapter.notifyDataSetChanged();
            }
        });
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
        if(broadcastReceiver != null){
            unregisterReceiver(broadcastReceiver);
        }
        transferService.shutdown();
        if(wifiP2pManager != null && Build.VERSION.SDK_INT >= 16) {
            wifiP2pManager.stopPeerDiscovery(channel, null);
        }
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

    //send over bluetooth and send over wifi button action
    public void sendButtonPressed(View view) {
        EditText inputTextBox = (EditText)findViewById(R.id.input_editText);
        textToSend = inputTextBox.getText().toString();
        if (textToSend.length()==0){
            inputTextBox.setError("enter any text");
        }
        if (view.getId() == R.id.send_with_bluetooth_button) {
//            Device bluetoothDevice = bluetoothDevices.get(currentSelection);
//            BluetoothClientThread clientThread = new BluetoothClientThread(bluetoothDevice.bluetoothDevice);
//            clientThread.start();
        }
    }

    //setting up the device list view adapter and item click events
    public void configureDeviceListView(){
        deviceListView = (ListView)findViewById(R.id.device_listView);
        combinedDeviceList = new ArrayList<>();
        deviceListAdapter = new DeviceListAdapter(this, combinedDeviceList);
        deviceListView.setAdapter(deviceListAdapter);
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                showSelectedDevice(i);
            }
        });
    }

    //shows the current selected device in green color. only one device can be selected at a time. selected = 1 means selected, selected = 0 means otherwise
    public void showSelectedDevice(int position) {
    }

    //shows the wifi p2p state
    public void wifiP2PState(int state) {
        if (state == 0)
            showAlert("WiFi p2p disabled");
    }

    //callback method from wifi direct broadcast receiver
    public void wifiDeviceDiscovered(WifiP2pDeviceList discoveredDevices) {
        for (WifiP2pDevice item : discoveredDevices.getDeviceList()
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
                Device device = new Device(item.deviceName, item.deviceAddress, 0, DEVICE_TYPE_WIFI,null);
                wifiDevices.add(device);
            }
        }
    }

    public void bluetoothDeviceDiscovered(BluetoothDevice device) {
        Device newDevice = new Device(device.getName(),device.getAddress(),0,1,device);
        int flag = 0;
        for (Device item:bluetoothDevices) {
            if (item.deviceAddress.equalsIgnoreCase(newDevice.deviceAddress)){
                flag = 1;
                break;
            }
        }
        if (flag == 0){
            bluetoothDevices.add(newDevice);
        }
    }

    public void manageConnectedBluetoothSocket(BluetoothSocket socket, int type){
        if (type == TYPE_SERVER) {
            connectedThread = new ConnectedThread(socket);
            connectedThread.start();
        }
        else if (type == TYPE_CLIENT) {
            connectedThread = new ConnectedThread(socket);
            connectedThread.write(textToSend.getBytes());
        }
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
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }

                if (socket != null) {
                    try {
                        // A connection was accepted. Perform work associated with
                        // the connection in a separate thread.
                        manageConnectedBluetoothSocket(socket, TYPE_SERVER);
                        mmServerSocket.close();
                        break;
                    } catch (IOException e) {

                    }
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

    //bluetooth client thread
    private class BluetoothClientThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public BluetoothClientThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));
            } catch (IOException e) {
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            manageConnectedBluetoothSocket(mmSocket, TYPE_CLIENT);
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }
    //end of bluetooth client thread

    //bluetooth data send/receive thread
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e("input stream error", "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e("output stream error", "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
                    numBytes = mmInStream.read(mmBuffer);
                    showReceivedData(mmBuffer);
                } catch (IOException e) {
                    Log.d("disconnection error", "Input stream was disconnected", e);
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e("sending error", "Error occurred when sending data", e);
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("socket closing error", "Could not close the connect socket", e);
            }
        }
    }

    public void onWifiP2PDeviceConnected(final WifiP2pInfo wifiInfo) {
        if(wifiInfo.isGroupOwner){
            transferService.startServer(8089);
        }
        else{
            //wait 2 seconds for server
            Toast.makeText(this,"Waiting for server ", Toast.LENGTH_SHORT).show();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    transferService.establishConnection(wifiInfo.groupOwnerAddress.getHostAddress(),8089);
                }
            },3000);

        }
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.send_with_wifi_button){
//            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//            intent.addCategory(Intent.CATEGORY_OPENABLE);
//            intent.setType("*/*");
//            startActivityForResult(intent, PICKFILE_REQUEST_CODE);
            //sending hard code file
            EditText inputTextBox = (EditText)findViewById(R.id.input_editText);
            textToSend = inputTextBox.getText().toString();
            String filename = textToSend + ".txt";
            File fileToSend = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), filename);
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(fileToSend);
                fileOutputStream.write(textToSend.getBytes());
                fileOutputStream.close();
            } catch (IOException e) {
            }
            transferService.sendFile(Environment.getExternalStorageDirectory().getAbsolutePath(),filename);
            Toast.makeText(this,"Sending file...",Toast.LENGTH_SHORT).show();
        }
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