package tausif.androidprojects.d2dcommwithdynamicrouting;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class WiFiDirectUDPSender extends Thread {
    private DatagramSocket socket;
    private DatagramPacket packet;

    WiFiDirectUDPSender() {
        try {
            socket = new DatagramSocket();
        }catch (IOException ex) {

        }
    }

    public void createPkt(String pktStr, InetAddress destAddr) {
        packet = new DatagramPacket(pktStr.getBytes(), pktStr.length(), destAddr, Constants.WiFiDirectUDPListeningPort);
    }

    @Override
    public void run() {
        try {
            socket.send(packet);
        }catch (IOException ex) {

        }
    }
}
