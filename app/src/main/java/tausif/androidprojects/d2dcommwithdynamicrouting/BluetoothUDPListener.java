package tausif.androidprojects.d2dcommwithdynamicrouting;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class BluetoothUDPListener extends Thread {
    private DatagramSocket listener;
    private DatagramPacket receivedPkt;
    public BluetoothUDPListener() {
        try {
            byte [] address = Constants.hostBluetoothAddress.getBytes();
            listener = new DatagramSocket(4000, InetAddress.getByAddress(address));
            byte [] receiveBuffer = new byte[1024];
            receivedPkt = new DatagramPacket(receiveBuffer, receiveBuffer.length);
        }catch (IOException ex) {

        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                listener.receive(receivedPkt);
                byte[] receivedData = receivedPkt.getData();
                String receivedStr = new String(receivedData);
                Log.d("data received", receivedStr);
            }catch (IOException ex) {

            }
        }
    }
}
