package tausif.androidprojects.d2dcommwithdynamicrouting;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class WiFiDirectUDPSender extends Thread {
    private DatagramSocket socket;
    private DatagramPacket packet;

    public void createSkt() {
        try {
            socket = new DatagramSocket();
        }catch (IOException ex) {

        }
    }

    public void createPkt(String pktStr) {
        packet = new DatagramPacket(pktStr.getBytes(), pktStr.length(), Constants.groupOwnerAddress, Constants.WiFiDirectUDPListeningPort);
    }

    @Override
    public void run() {
        try {
            socket.send(packet);
        }catch (IOException ex) {

        }
    }
}
