package tausif.androidprojects.d2dcommwithdynamicrouting;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.UUID;

public class BluetoothSender {
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
            socket = device.bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString(Constants.MY_UUID));
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
