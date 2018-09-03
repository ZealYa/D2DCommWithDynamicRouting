package tausif.androidprojects.d2dcommwithdynamicrouting;

import java.net.InetAddress;

public class Constants {
    public static String hostBluetoothAddress = "";
    public static String hostWifiAddress = "";

    public static InetAddress groupOwnerAddress;
    public static boolean isGroupOwner;
    public static int WiFiDirectUDPListeningPort = 9000;

    public static final int BLUETOOTH_DEVICE = 0;
    public static final int WIFI_DEVICE = 1;

    public static final int LOSS_RATIO_PKT_SIZE = 50;   //in bytes
    public static final int MAX_LOSS_RATIO_PKTS = 100;

    public static final int timeSlotLength = 15;    // in seconds

    public static final int RTT = 101;
    public static final int RTT_RET = 102;
    public static final int PKT_LOSS = 103;
    public static final int IP_MAC_SYNC_REC = 104;
    public static final int IP_MAC_SYNC_RET = 105;

    public static int noOfExps = 15;
    public static boolean willRecordRSSI = false;

    public static final int WIFI_DIRECT_CONNECTION = 1000;

    public static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION = 2000;
}
