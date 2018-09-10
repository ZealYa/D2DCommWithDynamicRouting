package tausif.androidprojects.d2dcommwithdynamicrouting;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

public class BluetoothReceiver {
    private final BluetoothSocket socket;
    private final InputStream inputStream;
    private byte[] readBuffer; // mmBuffer store for the stream

    public BluetoothReceiver(BluetoothSocket socket) {
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
