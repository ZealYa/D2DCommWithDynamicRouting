package tausif.androidprojects.d2dcommwithdynamicrouting;

import java.net.InetAddress;

public class Constants {
    public static String hostBluetoothAddress = "";
    public static String hostWifiAddress = "";

    public static final String MY_UUID = "e439084f-b7f1-460c-8a3f-d4cc883413e2";

    public static InetAddress groupOwnerAddress;
    public static boolean isGroupOwner;
    public static final int WI_FI_DIRECT_UDP_LISTENING_PORT = 9000;

    public static final int BLUETOOTH_DEVICE = 0;
    public static final int WIFI_DEVICE = 1;

    public static final int LOSS_RATIO_PKT_SIZE = 50;   //in bytes
    public static final int MAX_LOSS_RATIO_PKTS = 100;

    public static final int TIME_SLOT_LENGTH = 15;    // in seconds
    public static final int BT_DISCOVERABLE_LENGTH = 1800;  //in seconds

    public static final int RTT = 101;
    public static final int RTT_RET = 102;
    public static final int PKT_LOSS = 103;
    public static final int IP_MAC_SYNC_REC = 104;
    public static final int IP_MAC_SYNC_RET = 105;

    public static int NO_OF_EXPS = 10;
    public static boolean willRecordRSSI = false;

    public static final int WIFI_DIRECT_CONNECTION = 1000;

    public static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION = 2000;
    public static final int REQUEST_CODE_LOCATION = 2001;
}
