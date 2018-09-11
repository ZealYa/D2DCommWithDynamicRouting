package tausif.androidprojects.d2dcommwithdynamicrouting;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;

public class ConnectedSocketManager extends Thread {
    private final BluetoothSocket socket;
    private final InputStream inputStream;
    private byte[] readBuffer; // mmBuffer store for the stream
    private Device device;

    public ConnectedSocketManager(BluetoothSocket socket) {
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

    public void setDevice(Device device) {
        this.device = device;
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

    public void run() {
        readBuffer = new byte[1500];
        int numBytes; // bytes returned from read()

        // Keep listening to the InputStream until an exception occurs.
        while (true) {
            try {
                // Read from the InputStream.
                numBytes = inputStream.read(readBuffer);
                long receiveTime = Calendar.getInstance().getTimeInMillis();
//                processReceivedBTPkt(readBuffer, receiveTime, numBytes);
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
