package tausif.androidprojects.d2dcommwithdynamicrouting;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.UUID;

public class SocketConnector {
    private BluetoothSocket socket;
    private Device device;

    public void setDevice(Device device) {
        this.device = device;
    }

    public void setSocket(BluetoothSocket socket) {
        this.socket = socket;
    }

    public BluetoothSocket createSocket() {
        try {
            socket = device.bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString(Constants.MY_UUID));
        }catch (IOException sktCrt) {

        }
        try {
            socket.connect();
        }catch (IOException sktCnct) {
            try {
                socket.close();
                return null;
            }catch (IOException sktClse) {

            }
        }
        return socket;
    }
}
