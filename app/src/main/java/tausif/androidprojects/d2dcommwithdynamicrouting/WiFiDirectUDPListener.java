package tausif.androidprojects.d2dcommwithdynamicrouting;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class WiFiDirectUDPListener extends Thread {
    public HomeActivity homeActivity;
    byte[] receivedBytes;
    DatagramPacket receivedPkt;
    DatagramSocket socket;
    public WiFiDirectUDPListener(HomeActivity homeActivity) {
        this.homeActivity = homeActivity;
        receivedBytes = new byte[1024];
        receivedPkt = new DatagramPacket(receivedBytes, receivedBytes.length);
        try {
            socket = new DatagramSocket(Constants.WiFiDirectUDPListeningPort);
        }catch (IOException ex) {

        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                socket.receive(receivedPkt);
                final String receivedString = new String(receivedBytes, 0 ,receivedPkt.getLength());
                homeActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        homeActivity.showAlert(receivedString);
                    }
                });
            }catch (IOException ex) {

            }
        }
    }
}
