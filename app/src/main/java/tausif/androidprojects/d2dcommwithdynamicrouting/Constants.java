package tausif.androidprojects.d2dcommwithdynamicrouting;

import java.net.InetAddress;

public class Constants {
    public static String hostBluetoothAddress = "";
    public static String hostWifiAddress = "";
    public static InetAddress groupOwnerAddress;
    public static boolean isGroupOwner;
    public static int WiFiDirectUDPListeningPort = 9000;

    public static final int WIFI_DEVICE = 1;
    public static final int BLUETOOTH_DEVICE = 0;

    public static int RTT_PKT_SIZE = 225;  //in bytes
    public static final int LOSS_RATIO_PKT_SIZE = 50;   //in bytes
    public static final int MAX_LOSS_RATIO_PKTS = 100;

    public static final int timeSlotLength = 10;    // in seconds

    public static final int RTT = 1;
    public static final int RTT_RET = 2;
    public static final int PKT_LOSS = 3;
    public static final int IP_MAC_SYNC = 4;

    public static final int noOfRuns = 60;

    public static final int WIFI_DIRECT_CONNECTION = 0;
}
