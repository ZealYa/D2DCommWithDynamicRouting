package tausif.androidprojects.d2dcommwithdynamicrouting;

import android.bluetooth.BluetoothSocket;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;

public class BTConnectedSocketManager extends Thread {
    private final BluetoothSocket socket;
    private final InputStream inputStream;
    private Device device;
    private HomeActivity homeActivity;

    BTConnectedSocketManager(BluetoothSocket socket, HomeActivity homeActivity) {
        this.homeActivity = homeActivity;
        this.socket = socket;
        InputStream tmpIn = null;
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

    long sendPkt(String packet) {
        try {
            OutputStream outputStream = socket.getOutputStream();
            long rttStartTime = Calendar.getInstance().getTimeInMillis();
            outputStream.write(packet.getBytes());
            outputStream.flush();
            return rttStartTime;
        } catch (IOException writeEx) {
            return 0;
        }
    }

    public void run() {
        byte [] readBuffer = new byte[1500];
        int numBytes; // bytes returned from read()

        // Keep listening to the InputStream until an exception occurs.
        while (true) {
            try {
                // Read from the InputStream.
                numBytes = inputStream.read(readBuffer);
                long receiveTime = Calendar.getInstance().getTimeInMillis();
                homeActivity.processReceivedBTPkt(readBuffer, receiveTime);
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
