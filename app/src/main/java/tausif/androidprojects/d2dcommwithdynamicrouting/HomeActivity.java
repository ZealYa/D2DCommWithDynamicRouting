package tausif.androidprojects.d2dcommwithdynamicrouting;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pInfo;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

import tausif.androidprojects.files.TransferService;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int PICKFILE_REQUEST_CODE = 323;
    ArrayList<Device> combinedDeviceList;
    ListView deviceListView;
    DeviceListAdapter deviceListAdapter;
    int TYPE_SERVER = 0;
    int TYPE_CLIENT = 1;
    private TransferService transferService;
    private Handler handler;
    String NAME = "server";
    String MY_UUID = "e439084f-b7f1-460c-8a3f-d4cc883413e2";
    ConnectedThread connectedThread;
    BluetoothServerThread bluetoothServerThread;
    BluetoothClientThread bluetoothClientThread;
    String textToSend = "";
    BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        handler = new Handler();
        transferService = new TransferService(this);
        findViewById(R.id.send_with_wifi_button).setOnClickListener(this);
        hasStorageWriteAccess();
    }

    //setting up the device list view adapter and item click events
    public void configureDeviceListView(){
        deviceListView = findViewById(R.id.device_listView);
        combinedDeviceList = new ArrayList<>();
        deviceListAdapter = new DeviceListAdapter(this, combinedDeviceList);
        deviceListView.setAdapter(deviceListAdapter);
    }

    //configures the bluetooth and wifi discovery options and starts the background process for discovery
    public void startDiscovery(View view){
        bluetoothClientThread = new BluetoothClientThread();
        configureDeviceListView();
//        configureBluetoothDataTransfer();
        PeerDiscoveryController peerDiscoveryController = new PeerDiscoveryController(this, this);
    }

    public void configureBluetoothDataTransfer() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothServerThread = new BluetoothServerThread();
        bluetoothServerThread.start();
    }

    //callback method from peer discovery controller after finishing a cycle of wifi and bluetooth discovery
    public void discoveryFinished(ArrayList<Device> wifiDevices, ArrayList<Device> bluetoothDevices) {
        if (combinedDeviceList.size() > 0)
            combinedDeviceList.clear();
        Device dummyWifiDevice = new Device(Constants.WIFI_DEVICE, null, null, 0);
        combinedDeviceList.add(dummyWifiDevice);
        combinedDeviceList.addAll(wifiDevices);
        Device dummyBluetoothDevice = new Device(Constants.BLUETOOTH_DEVICE, null, null, 0);
        combinedDeviceList.add(dummyBluetoothDevice);
        combinedDeviceList.addAll(bluetoothDevices);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                deviceListAdapter.notifyDataSetChanged();
            }
        });
        measureBluetoothRTT();
    }

    public void measureBluetoothRTT() {
        for (Device device: combinedDeviceList
             ) {
            if (device.deviceType == Constants.BLUETOOTH_DEVICE && device.bluetoothDevice != null && device.bluetoothDevice.getName().contains("NWSL")) {
                String packet = PacketManager.createRTTPacket(Constants.timeSlotCount, Constants.hostBluetoothAddress, device.bluetoothDevice.getAddress());
                bluetoothClientThread.setDevice(device.bluetoothDevice);
                bluetoothClientThread.setPacket(packet);
                bluetoothClientThread.setSocket();
                bluetoothClientThread.start();
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
        connectedThread.cancel();
    }

    //send over bluetooth and send over wifi button action
    public void sendButtonPressed(View view) {
//        EditText inputTextBox = (EditText)findViewById(R.id.input_editText);
//        textToSend = inputTextBox.getText().toString();
//        if (textToSend.length()==0){
//            inputTextBox.setError("enter any text");
//        }
        if (view.getId() == R.id.send_with_bluetooth_button) {
//            Device bluetoothDevice = bluetoothDevices.get(currentSelection);
//            BluetoothClientThread clientThread = new BluetoothClientThread(bluetoothDevice.bluetoothDevice);
//            clientThread.start();
            configureBluetoothDataTransfer();
        }
    }

    //shows the wifi p2p state
    public void wifiP2PState(int state) {
        if (state == 0)
            showAlert("WiFi p2p disabled");
    }

    public void manageConnectedBluetoothSocket(BluetoothSocket socket, int type, String packet){
        if (type == TYPE_SERVER) {
            connectedThread = new ConnectedThread(socket);
            connectedThread.start();
        }
//        else if (type == TYPE_CLIENT) {
//            connectedThread = new ConnectedThread(socket);
//            connectedThread.write(packet.getBytes());
//        }
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
//                    try {
                        // A connection was accepted. Perform work associated with
                        // the connection in a separate thread.
                        manageConnectedBluetoothSocket(socket, TYPE_SERVER, "");
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

    //bluetooth client thread
    private class BluetoothClientThread extends Thread {
//        private final BluetoothSocket mmSocket;
//        private final BluetoothDevice mmDevice;
        private BluetoothSocket socket;
        private BluetoothDevice device;
        private String packet;

//        public BluetoothClientThread(BluetoothDevice device) {
//            // Use a temporary object that is later assigned to mmSocket
//            // because mmSocket is final.
//            BluetoothSocket tmp = null;
//            mmDevice = device;
//
//            try {
//                // Get a BluetoothSocket to connect with the given BluetoothDevice.
//                // MY_UUID is the app's UUID string, also used in the server code.
//                tmp = device.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));
//            } catch (IOException e) {
//            }
//            mmSocket = tmp;
//        }

        public void setDevice(BluetoothDevice device) {
            this.device = device;
        }

        public void setPacket(String packet) {
            this.packet = packet;
        }

        public void setSocket() {
            try {
                socket = device.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));
            } catch (IOException createEx) {

            }
        }

//        public void run() {
//            try {
//                // Connect to the remote device through the socket. This call blocks
//                // until it succeeds or throws an exception.
//                mmSocket.connect();
//            } catch (IOException connectException) {
//                // Unable to connect; close the socket and return.
//                try {
//                    mmSocket.close();
//                } catch (IOException closeException) {
//                }
//                return;
//            }
//
//            // The connection attempt succeeded. Perform work associated with
//            // the connection in a separate thread.
//            manageConnectedBluetoothSocket(mmSocket, TYPE_CLIENT, packet);
//        }


        @Override
        public void run() {
            try {
                socket.connect();
            } catch (IOException connectEx) {
                try {
                    socket.close();
                } catch (IOException closeEx) {

                }
                return;
            }
//            manageConnectedBluetoothSocket(socket, TYPE_CLIENT, packet);
            try {
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(packet.getBytes());
                this.cancel();
            } catch (IOException ex) {

            }

        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                socket.close();
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
//        public void write(byte[] bytes) {
//            try {
//                mmOutStream.write(bytes);
//            } catch (IOException e) {
//                Log.e("sending error", "Error occurred when sending data", e);
//            }
//        }

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