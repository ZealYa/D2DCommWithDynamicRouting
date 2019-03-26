package tausif.androidprojects.d2dcommwithdynamicrouting;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class WDUDPSender extends Thread {
    private DatagramSocket socket;
    private DatagramPacket packet;
    private boolean runLoop;
    private int noOfPktsToSend;

    WDUDPSender() {
        try {
            socket = new DatagramSocket();
        }catch (IOException ex) {

        }
    }

    public void createPkt(String pktStr, InetAddress destAddr) {
        packet = new DatagramPacket(pktStr.getBytes(), pktStr.length(), destAddr, Constants.WI_FI_DIRECT_UDP_LISTENING_PORT);
    }

    public void setRunLoop(boolean runLoop) {
        this.runLoop = runLoop;
    }

    public void setNoOfPktsToSend(int noOfPktsToSend) {
        this.noOfPktsToSend = noOfPktsToSend;
    }

    @Override
    public void run() {
        try {
            if (runLoop) {
                for (int i=0;i<noOfPktsToSend;i++)
                    socket.send(packet);
            }
            else
                socket.send(packet);
        }catch (IOException ex) {

        }
    }
}
