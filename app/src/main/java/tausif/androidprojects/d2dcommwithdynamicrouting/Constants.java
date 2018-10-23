package tausif.androidprojects.d2dcommwithdynamicrouting;

import java.net.InetAddress;

public class Constants {
    static String hostBluetoothName = "";
    static final String MY_UUID = "e439084f-b7f1-460c-8a3f-d4cc883413e2";

    static String hostWifiName = "";
    static String hostWifiAddress = "";
    static InetAddress groupOwnerAddress;
    static boolean isGroupOwner;
    static final int WI_FI_DIRECT_UDP_LISTENING_PORT = 9000;

    static final int BLUETOOTH_DEVICE = 0;
    static final int WIFI_DEVICE = 1;

    static final int LOSS_RATIO_PKT_SIZE = 50;   //in bytes
    static final int MAX_LOSS_RATIO_PKTS = 100;

    static final int TIME_SLOT_LENGTH = 10;    // in seconds
    static final int BT_DISCOVERABLE_LENGTH = 1800;  //in seconds

    static final int RTT = 101;
    static final int RTT_RET = 102;
    static final int PKT_LOSS = 103;
    static final int IP_MAC_SYNC = 104;
    static final int IP_MAC_SYNC_RET = 105;
    static final int UDP_THROUGHPUT = 106;
    static final int UDP_THROUGHPUT_RET = 107;
    static final int FILE_TRANSFER = 108;

    static int EXP_NO = 0;
    static final int MAX_NO_OF_EXPS = 10;

    static final int WIFI_DIRECT_CONNECTION = 1000;
    static final int BLUETOOTH_CONNECTION = 1001;

    static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION = 2000;
    static final int REQUEST_CODE_LOCATION = 2001;
}
