package tausif.androidprojects.d2dcommwithdynamicrouting;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class BluetoothUDPSender {
    public void sendPkt(String pkt, String destAddress) {
        try {
            DatagramSocket sender = new DatagramSocket();
            byte [] address = destAddress.getBytes();
            sender.connect(InetAddress.getByAddress(address), 4000);
            byte [] data = pkt.getBytes();
            DatagramPacket pktToSend = new DatagramPacket(data, data.length);
            sender.send(pktToSend);
        }catch (IOException ex) {

        }
    }
}
